package com.example.fbservice.content;

import com.example.fbservice.config.TrafficLimiter;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.Arrays;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/ai")
public class AiContentController {
    private final AiContentService service;
    private final TrafficLimiter limiter;
    public AiContentController(AiContentService service,TrafficLimiter limiter){
        this.service=service;this.limiter=limiter;
    }
    /** Authenticated capability discovery; never expose the configured provider key. */
    @GetMapping("/capabilities")
    public Map<String,Object> capabilities() {
        boolean configured=service.isConfigured();
        return Map.of(
            "mode",configured?"GENERATIVE_AI_CONFIGURED":"LOCAL_TEMPLATES_ONLY",
            "providerConfigured",configured,
            "supportedTasks",Arrays.stream(AiContentTask.values()).map(Enum::name).toList(),
            "supportedLanguages",Arrays.stream(AiContentLanguage.values()).map(Enum::name).toList(),
            "perUserPerMinute",5,
            "perUserPer24Hours",25,
            "unconfiguredRequestsPerMinute",30,
            "quotaScope","PER_INSTANCE_ONLY",
            "publishingEnabled",false
        );
    }

    @PostMapping({"/caption","/generate"})
    public Map<String,String> generate(@Valid @RequestBody AiContentRequest input,Principal principal){
        // Missing provider credentials must not consume the paid-AI request budget.
        // Keep a separate conservative anti-spam limit even while generation is unavailable.
        if (!service.isConfigured()) {
            limiter.check("ai:unconfigured:"+principal.getName(),30,Duration.ofMinutes(1));
            return service.generate(input); // Returns 503; never calls the provider.
        }
        // Defense-in-depth per-instance budget (production needs a shared atomic quota store).
        limiter.check("ai:daily:"+principal.getName(),25,Duration.ofDays(1));
        limiter.check("ai:"+principal.getName(),5,Duration.ofMinutes(1));
        return service.generate(input);
    }
}
