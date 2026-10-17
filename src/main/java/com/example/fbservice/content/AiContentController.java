package com.example.fbservice.content;

import com.example.fbservice.config.TrafficLimiter;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/ai")
public class AiContentController {
    private final AiContentService service;
    private final TrafficLimiter limiter;
    public AiContentController(AiContentService service,TrafficLimiter limiter){
        this.service=service;this.limiter=limiter;
    }
    @PostMapping("/caption")
    public Map<String,String> generate(@Valid @RequestBody AiContentRequest input,Principal principal){
        limiter.check("ai:"+principal.getName(),5,Duration.ofMinutes(1));
        return service.generate(input);
    }
}
