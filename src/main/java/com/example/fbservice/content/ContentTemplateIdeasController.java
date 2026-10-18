package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import com.example.fbservice.config.TrafficLimiter;
import java.security.Principal;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Offline, authenticated, read-only content outlines with no provider credentials. */
@RestController
@RequestMapping("/api/v1/content-templates")
public class ContentTemplateIdeasController {
    private final ContentTemplateIdeasService service;
    private final TrafficLimiter limiter;
    public ContentTemplateIdeasController(ContentTemplateIdeasService service,TrafficLimiter limiter) {
        this.service=service;
        this.limiter=limiter;
    }

    @GetMapping("/ideas")
    public ContentTemplateIdeasService.IdeaTemplate ideas(
            @RequestParam SocialPlatform platform,
            @RequestParam String topic,
            @RequestParam AiContentTask task,
            @RequestParam(defaultValue="VI") AiContentLanguage language,
            Principal principal) {
        limiter.check("content-ideas:"+principal.getName(),20,Duration.ofMinutes(1));
        try {
            return service.ideas(platform,topic,task,language);
        } catch(IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,ex.getMessage());
        }
    }
}
