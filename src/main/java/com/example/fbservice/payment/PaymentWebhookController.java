package com.example.fbservice.payment;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {
    private final PaymentWebhookService service;
    public PaymentWebhookController(PaymentWebhookService service){this.service=service;}
    @PostMapping(value="/internal",consumes=MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> receive(@RequestBody String body,
        @RequestHeader(value="X-GrowLab-Signature",required=false) String signature){
        return service.accept(body,signature);
    }
}
