package com.example.fbservice.integration;

import com.example.fbservice.config.TrafficLimiter;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/integrations/youtube")
public class YouTubePublicController {
    private final YouTubePublicChannelService service;
    private final TrafficLimiter limiter;
    public YouTubePublicController(YouTubePublicChannelService service,TrafficLimiter limiter){
        this.service=service;this.limiter=limiter;
    }
    @GetMapping("/channels/{id}")
    public Map<String,Object> getPublicChannel(@PathVariable String id,Principal principal){
        limiter.check("youtube:"+principal.getName(),20,Duration.ofMinutes(1));
        try{return service.channelSummary(id);}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}
    }
}
