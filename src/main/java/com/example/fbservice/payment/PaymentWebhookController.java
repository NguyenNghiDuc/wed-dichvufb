package com.example.fbservice.payment;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {
    // Bound the transport read itself. A service-level length check is too late
    // when a request has already been buffered into an unbounded @RequestBody.
    private static final int MAX_WEBHOOK_BODY_BYTES = 4096;
    private final PaymentWebhookService service;

    public PaymentWebhookController(PaymentWebhookService service) {
        this.service = service;
    }

    @PostMapping(value = "/internal", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> receive(HttpServletRequest request,
            @RequestHeader(value = "X-GrowLab-Signature", required = false) String signature,
            @RequestHeader(value = "X-GrowLab-Timestamp", required = false) String timestamp) {
        // Reject repeated authentication headers before reading even one byte.
        // A reverse proxy and application must never select different header
        // values for the same HMAC-authenticated request.
        if (java.util.Collections.list(request.getHeaders("X-GrowLab-Timestamp")).size() != 1
                || java.util.Collections.list(request.getHeaders("X-GrowLab-Signature")).size() != 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ambiguous webhook headers");
        }
        if (request.getContentLengthLong() > MAX_WEBHOOK_BODY_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Webhook payload too large");
        }
        final byte[] body;
        try {
            // Read one extra byte to detect oversized chunked transfers even if
            // Content-Length is absent or untrusted. Never hold more than 4097 bytes.
            body = request.getInputStream().readNBytes(MAX_WEBHOOK_BODY_BYTES + 1);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unreadable webhook body");
        }
        if (body.length > MAX_WEBHOOK_BODY_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Webhook payload too large");
        }
        return service.accept(body, signature, timestamp);
    }
}
