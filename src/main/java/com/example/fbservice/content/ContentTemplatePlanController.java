package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import com.example.fbservice.config.TrafficLimiter;
import java.security.Principal;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Same-origin authenticated template lookup; no LLM or social-media API. */
@RestController
@RequestMapping("/api/v1/content-templates")
public class ContentTemplatePlanController {
    private final ContentTemplatePlanService templates;
    private final TrafficLimiter limiter;
    public ContentTemplatePlanController(ContentTemplatePlanService templates,TrafficLimiter limiter) {
        this.templates=templates;
        this.limiter=limiter;
    }

    @GetMapping("/plan")
    public ContentTemplatePlanService.TemplatePlan plan(@RequestParam SocialPlatform platform,
                                                         @RequestParam String topic,
                                                         @RequestParam(defaultValue="7") int days,
                                                         Principal principal) {
        limiter.check("content-plan:"+principal.getName(),20,Duration.ofMinutes(1));
        try {
            return templates.plan(platform,topic,days);
        } catch(IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,ex.getMessage());
        }
    }
}
