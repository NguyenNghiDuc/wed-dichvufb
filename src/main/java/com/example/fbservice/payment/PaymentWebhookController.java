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
    public Map<String,Object> receive(jakarta.servlet.http.HttpServletRequest request,
        @RequestHeader(value="X-GrowLab-Signature",required=false) String signature) throws java.io.IOException {
        if(request.getContentLengthLong()>4096)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
        byte[] bytes=request.getInputStream().readNBytes(4097);
        if(bytes.length>4096)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
        return service.accept(new String(bytes,java.nio.charset.StandardCharsets.UTF_8),signature);
    }
}
