package com.example.fbservice.payment;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/** Public only for provider callbacks; service rejects unsigned requests. */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class CassoV2WebhookController {
    private final CassoV2WebhookService service;
    public CassoV2WebhookController(CassoV2WebhookService service){this.service=service;}

    @PostMapping(value="/casso-v2",consumes=MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> receive(@RequestBody String rawBody,
        @RequestHeader(value="X-Casso-Signature",required=false) String signature){
        return service.accept(rawBody,signature);
    }
}
