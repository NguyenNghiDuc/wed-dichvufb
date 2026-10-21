package com.example.fbservice.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provider-specific Casso V2 adapter. Signed payload must be an independently
 * synchronized bank event, not an invented transfer claim from a browser.
 */
@Service
public class CassoV2WebhookService {
    private static final Pattern HEADER=Pattern.compile("t=([0-9]{13}),v1=([a-fA-F0-9]{128})");
    private final boolean enabled;
    private final boolean liveReady;
    private final String secret;
    private final String destinationAccount;
    private final ObjectMapper json;
    private final BankSettlementWebhookService settlement;

    public CassoV2WebhookService(
        @Value("#{environment.getProperty('growth.payments.casso.enabled', 'false')}") boolean enabled,
        @Value("#{environment.getProperty('growth.payments.live-ready', 'false')}") boolean liveReady,
        @Value("#{environment.getProperty('growth.payments.casso.webhook-secret', '')}") String secret,
        @Value("#{environment.getProperty('growth.payments.bank.account-number', '')}") String account,
        ObjectMapper json, BankSettlementWebhookService settlement){
        this.enabled=enabled; this.liveReady=liveReady; this.secret=secret; this.destinationAccount=account.trim();
        this.json=json; this.settlement=settlement;
    }

    public Map<String,Object> accept(String body,String signature){
        if(!enabled||!liveReady||secret.length()<32||!destinationAccount.matches("[0-9]{6,24}"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Casso bank adapter is not activated");
        if(body==null||body.length()>8192||signature==null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid signed webhook");
        Matcher header=HEADER.matcher(signature);
        if(!header.matches())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid signature");
        long timestamp;
        try{timestamp=Long.parseLong(header.group(1));}
        catch(NumberFormatException ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
        Instant now=Instant.now(),signed=Instant.ofEpochMilli(timestamp);
        // Casso retries up to 24h. Keep a small buffer, yet reject old/future replay windows.
        if(signed.isBefore(now.minus(Duration.ofHours(26)))||signed.isAfter(now.plus(Duration.ofMinutes(5))))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Expired signature");
        JsonNode root;
        try{root=json.readTree(body);}
        catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid JSON");}
        if(root==null||!root.isObject())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Casso event");
        try{
            String canonical=json.writeValueAsString(sort(root));
            String signedData=timestamp+"."+canonical;
            Mac mac=Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
            byte[] expected=mac.doFinal(signedData.getBytes(StandardCharsets.UTF_8));
            if(!MessageDigest.isEqual(expected,HexFormat.of().parseHex(header.group(2))))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid provider signature");
        }catch(ResponseStatusException ex){throw ex;}
         catch(Exception ex){throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Signature verification failed");}

        JsonNode event=root.path("data");
        if(!root.path("error").isIntegralNumber()||root.path("error").longValue()!=0
            ||!event.isObject()||!event.path("id").isIntegralNumber()
            ||event.path("id").longValue()<=0
            ||!event.path("amount").isIntegralNumber()
            ||event.path("amount").longValue()<1000
            ||event.path("amount").longValue()>1000000000L
            ||!event.path("bankAbbreviation").asText("").equalsIgnoreCase("TCB")
            ||!destinationAccount.equals(event.path("accountNumber").asText(""))
            ||event.path("description").asText("").isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Unmatched incoming Techcombank event");
        }
        String providerTxId="casso-"+event.path("id").longValue();
        BankSettlementEvent verified=new BankSettlementEvent(providerTxId,
            providerTxId,event.path("amount").longValue(),destinationAccount,
            event.path("description").asText(),"SETTLED");
        var result=settlement.creditVerified(verified);
        return Map.of("success",true,"accepted",true,"duplicate",result.get("duplicate"));
    }

    /** Sort all object keys recursively to match Casso's published HMAC-SHA512 sample. */
    public static Object sort(JsonNode value){
        if(value.isObject()){
            Map<String,Object> map=new TreeMap<>();
            Iterator<Map.Entry<String,JsonNode>> fields=value.fields();
            while(fields.hasNext()){
                var item=fields.next();
                map.put(item.getKey(),sort(item.getValue()));
            }
            return map;
        }
        if(value.isArray()){
            List<Object> values=new ArrayList<>();
            value.forEach(child->values.add(sort(child)));
            return values;
        }
        if(value.isNull())return null;
        if(value.isTextual())return value.textValue();
        if(value.isBoolean())return value.booleanValue();
        if(value.isIntegralNumber())return value.numberValue();
        if(value.isFloatingPointNumber())return value.decimalValue();
        throw new IllegalArgumentException("Unsupported JSON node");
    }
}
