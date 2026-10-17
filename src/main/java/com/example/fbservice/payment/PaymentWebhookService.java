package com.example.fbservice.payment;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.notification.*;
import com.example.fbservice.repository.DepositRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentWebhookService {
    private final String secret;
    private final DepositRequestRepository deposits;
    private final PaymentReceiptRepository receipts;
    private final AccountNotificationRepository notifications;
    private final ObjectMapper json;

    public PaymentWebhookService(@Value("${growth.payments.webhook-secret:}") String secret,
       DepositRequestRepository deposits,PaymentReceiptRepository receipts,
       AccountNotificationRepository notifications,ObjectMapper json){
        this.secret=secret;this.deposits=deposits;this.receipts=receipts;
        this.notifications=notifications;this.json=json;
    }

    @Transactional
    public Map<String,Object> accept(String body,String providedSignature){
        if(secret==null||secret.length()<32)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Webhook not configured");
        if(body==null||body.length()>4096)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if(providedSignature==null||!providedSignature.matches("[a-fA-F0-9]{64}"))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            byte[] expected=mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] actual=HexFormat.of().parseHex(providedSignature);
            if(!MessageDigest.isEqual(expected,actual))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }catch(ResponseStatusException ex){throw ex;}
         catch(Exception ex){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);}

        PaymentWebhookEvent event;
        try{event=json.readValue(body,PaymentWebhookEvent.class);}
        catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid payment event");}
        if(event==null||event.eventId()==null||!event.eventId().matches("[a-zA-Z0-9_-]{8,100}")||
           event.depositRequestId()==null||event.depositRequestId()<1||
           event.amountVnd()==null||event.amountVnd()<1000||
           !"SETTLED".equals(event.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid settlement event");

        // Serialize concurrent settlements of the same deposit.
        DepositRequest deposit=deposits.findLockedById(event.depositRequestId())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existing=receipts.findByEventId(event.eventId());
        if(existing.isPresent()){
            PaymentReceipt receipt=existing.get();
            if(!receipt.getDepositRequestId().equals(deposit.getId())||
               receipt.getAmountVnd()!=event.amountVnd())
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Event ID reused with different details");
            return Map.of("accepted",true,"duplicate",true);
        }
        if(deposit.getAmount()!=event.amountVnd())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Amount mismatch");
        if(!"PENDING".equals(deposit.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Deposit already processed");
        receipts.save(new PaymentReceipt(event.eventId(),deposit.getId(),event.amountVnd()));
        deposit.setStatus("APPROVED");
        notifications.save(new AccountNotification(deposit.getOwnerUsername(),"Yêu cầu nạp tiền #"+deposit.getId(),
            "Đã xác nhận thông điệp thanh toán được ký bởi tích hợp nội bộ. Chưa hỗ trợ cộng số dư."));
        return Map.of("accepted",true,"duplicate",false);
    }
}
