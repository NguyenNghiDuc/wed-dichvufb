package com.example.fbservice.payment;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.notification.AccountNotification;
import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentWebhookService {
    private static final long MAX_CLOCK_SKEW_SECONDS = 300;
    private final String secret;
    private final DepositRequestRepository deposits;
    private final PaymentReceiptRepository receipts;
    private final AccountNotificationRepository notifications;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;

    public PaymentWebhookService(@Value("${growth.payments.webhook-secret:}") String secret,
            DepositRequestRepository deposits, PaymentReceiptRepository receipts,
            AccountNotificationRepository notifications, ObjectMapper json,
            PlatformTransactionManager transactionManager) {
        this.secret = secret;
        this.deposits = deposits;
        this.receipts = receipts;
        this.notifications = notifications;
        this.json = json;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * Internal sandbox protocol, NOT a VNPay signature or settlement callback.
     * Sign the exact wire bytes: HMAC_SHA256(secret, ascii(epochSeconds) + "." + rawBody).
     * Verify before JSON decoding and before any database mutation.
     */
    public Map<String, Object> accept(byte[] body, String providedSignature, String timestamp) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook not configured");
        }
        if (body == null || body.length == 0 || body.length > 4096) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook size");
        }
        if (timestamp == null || !timestamp.matches("[0-9]{10,12}")
                || providedSignature == null || !providedSignature.matches("[a-fA-F0-9]{64}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        final long eventTime;
        try {
            eventTime = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        long now = Instant.now().getEpochSecond();
        if (eventTime < now - MAX_CLOCK_SKEW_SECONDS || eventTime > now + MAX_CLOCK_SKEW_SECONDS) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Stale webhook timestamp");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(timestamp.getBytes(StandardCharsets.US_ASCII));
            mac.update((byte) '.');
            byte[] expected = mac.doFinal(body);
            byte[] actual = HexFormat.of().parseHex(providedSignature);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Parse the signed body ONCE and enforce four exact JSON types.
        // Jackson's default scalar coercions must not turn "50000", 50000.9,
        // or an out-of-range integer into a valid settlement amount.
        com.fasterxml.jackson.databind.JsonNode payload;
        try (JsonParser parser = json.getFactory().createParser(body)) {
            parser.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            payload = json.readTree(parser);
            if (parser.nextToken() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trailing payment data");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment event");
        }
        if (payload == null || !payload.isObject() || payload.size() != 4
                || !payload.path("eventId").isTextual()
                || !payload.path("status").isTextual()
                || !payload.path("depositRequestId").isIntegralNumber()
                || !payload.path("depositRequestId").canConvertToLong()
                || !payload.path("amountVnd").isIntegralNumber()
                || !payload.path("amountVnd").canConvertToLong()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment fields");
        }
        PaymentWebhookEvent event = new PaymentWebhookEvent(
                payload.get("eventId").textValue(),
                payload.get("depositRequestId").longValue(),
                payload.get("amountVnd").longValue(),
                payload.get("status").textValue());
        if (event.eventId() == null
                || !event.eventId().matches("[a-zA-Z0-9_-]{8,100}")
                || event.depositRequestId() == null || event.depositRequestId() < 1
                || event.amountVnd() == null || event.amountVnd() < 1000
                || event.amountVnd() > 1000000000L
                || !"SETTLED".equals(event.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid settlement event");
        }

        // Bad signatures and malformed events never acquire a database transaction.
        return transactions.execute(status -> settle(event));
    }

    private Map<String, Object> settle(PaymentWebhookEvent event) {
        // Lock the deposit first, so concurrent callbacks for one deposit are serialized.
        DepositRequest deposit = deposits.findLockedById(event.depositRequestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existing = receipts.findByEventId(event.eventId());
        if (existing.isPresent()) {
            PaymentReceipt receipt = existing.get();
            if (!receipt.getDepositRequestId().equals(deposit.getId())
                    || receipt.getAmountVnd() != event.amountVnd()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Event ID reused with different details");
            }
            // A retry is only safe to acknowledge if the persisted deposit is
            // still consistent with the receipt. A manual status/amount edit
            // must raise a conflict rather than falsely reporting acceptance.
            if (!"APPROVED".equals(deposit.getStatus())
                    || deposit.getAmount() != receipt.getAmountVnd()
                    || deposit.getOwnerUsername() == null
                    || deposit.getOwnerUsername().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Receipt and deposit out of sync");
            }
            return Map.of("accepted", true, "duplicate", true);
        }
        if (deposit.getAmount() != event.amountVnd()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Amount mismatch");
        }
        if (!"PENDING".equals(deposit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit already processed");
        }
        if (deposit.getOwnerUsername() == null || deposit.getOwnerUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit has no owner");
        }

        // Database UNIQUE(event_id) and UNIQUE(deposit_request_id) are the final race guard.
        // Flush here to turn a cross-deposit event collision into a controlled conflict.
        try {
            receipts.saveAndFlush(new PaymentReceipt(event.eventId(), deposit.getId(), event.amountVnd()));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate settlement reference");
        }
        deposit.setStatus("APPROVED");
        notifications.save(new AccountNotification(deposit.getOwnerUsername(),
                "Yêu cầu nạp tiền #" + deposit.getId(),
                "Đã xác nhận thông điệp thanh toán được ký bởi tích hợp nội bộ. Chưa hỗ trợ cộng số dư."));
        return Map.of("accepted", true, "duplicate", false);
    }
}
