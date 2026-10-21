package com.example.fbservice.payment;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.notification.AccountNotification;
import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BankSettlementWebhookService {
    private static final Pattern REFERENCE=
        Pattern.compile("(?:^|[^A-Z0-9])GLNAP([1-9][0-9]{0,17})(?=$|[^A-Z0-9])");
    private final String secret;
    private final boolean internalEnabled;
    private final String destinationAccount;
    private final DepositRequestRepository deposits;
    private final PaymentReceiptRepository receipts;
    private final WalletCreditRepository credits;
    private final AccountNotificationRepository notifications;
    private final ObjectMapper json;

    public BankSettlementWebhookService(
        @Value("#{environment.getProperty('growth.payments.bank-webhook-secret', '')}") String secret,
        @Value("#{environment.getProperty('growth.payments.internal-verified-enabled', 'false')}") boolean internalEnabled,
        @Value("#{environment.getProperty('growth.payments.bank.account-number', '')}") String account,
        DepositRequestRepository deposits,PaymentReceiptRepository receipts,
        WalletCreditRepository credits,AccountNotificationRepository notifications,ObjectMapper json) {
        this.secret=secret; this.internalEnabled=internalEnabled; this.destinationAccount=account.trim();
        this.deposits=deposits; this.receipts=receipts; this.credits=credits;
        this.notifications=notifications; this.json=json;
    }

    @Transactional
    public Map<String,Object> accept(String body,String providedSignature){
        // Fail closed: only a trusted, independently verified bank/PSP adapter gets this secret.
        // An HMAC on an unverified customer claim is NOT evidence of a settled transfer.
        if(!internalEnabled||secret==null||secret.length()<32||!destinationAccount.matches("[0-9]{6,24}"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Bank settlement adapter not configured");
        if(body==null||body.length()>8192)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid payload size");
        if(providedSignature==null||!providedSignature.matches("[a-fA-F0-9]{64}"))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try{
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            if(!MessageDigest.isEqual(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)),
                HexFormat.of().parseHex(providedSignature)))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }catch(ResponseStatusException ex){throw ex;}
         catch(Exception ex){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);}

        BankSettlementEvent event;
        try{event=json.readValue(body,BankSettlementEvent.class);}
        catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid settlement event");}
        return creditVerified(event);
    }

    /**
     * Invoke ONLY after a provider-specific adapter validates the provider's
     * signed webhook. This method has no HTTP mapping.
     */
    @Transactional
    public Map<String,Object> creditVerified(BankSettlementEvent event){
        if(event==null||event.eventId()==null||!event.eventId().matches("[A-Za-z0-9_-]{8,100}")||
           event.transactionId()==null||!event.transactionId().matches("[A-Za-z0-9_-]{8,100}")||
           event.amountVnd()==null||event.amountVnd()<1000||event.amountVnd()>1000000000L||
           event.description()==null||event.description().length()>1024||
           !"SETTLED".equals(event.status())||!destinationAccount.equals(event.destinationAccount()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid verified settlement");

        Long depositId=depositIdFromDescription(event.description());
        var previous=credits.findByBankTransactionId(event.transactionId());
        if(previous.isPresent()){
            var c=previous.get();
            if(!c.getDepositRequestId().equals(depositId)||c.getAmountVnd()!=event.amountVnd())
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Bank transaction reused");
            return Map.of("accepted",true,"duplicate",true);
        }
        // Lock prevents two different bank transactions from crediting the SAME deposit.
        DepositRequest deposit=deposits.findLockedById(depositId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Unmatched reference"));
        if(deposit.getAmount()!=event.amountVnd())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Amount mismatch");
        if(!"PENDING".equals(deposit.getStatus())||credits.existsByDepositRequestId(depositId))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Deposit already processed");
        if(receipts.findByEventId(event.eventId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Event ID reused");
        // Atomic ledger credit, receipt, state and notice; UNIQUE constraints are the final guard.
        receipts.save(new PaymentReceipt(event.eventId(),depositId,event.amountVnd()));
        credits.save(new WalletCredit(deposit.getOwnerUsername(),depositId,event.transactionId(),event.amountVnd()));
        deposit.setStatus("APPROVED");
        notifications.save(new AccountNotification(deposit.getOwnerUsername(),"Đã ghi có ví #"+depositId,
            "Đã đối soát giao dịch ngân hàng và ghi có "+event.amountVnd()+" VNĐ vào ví."));
        return Map.of("accepted",true,"duplicate",false);
    }
    private Long depositIdFromDescription(String description){
        Matcher matcher=REFERENCE.matcher(description.toUpperCase(Locale.ROOT));
        if(!matcher.find())throw new ResponseStatusException(HttpStatus.CONFLICT,"Unmatched transfer reference");
        String number=matcher.group(1);
        if(matcher.find())throw new ResponseStatusException(HttpStatus.CONFLICT,"Ambiguous transfer reference");
        try{return Long.valueOf(number);}
        catch(NumberFormatException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid reference");}
    }
}
