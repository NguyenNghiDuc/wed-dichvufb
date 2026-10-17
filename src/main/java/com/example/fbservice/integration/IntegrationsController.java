package com.example.fbservice.integration;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationsController {
    private final boolean youtubeConfigured;

    public IntegrationsController(@Value("${growth.youtube.api-key:}") String youtubeKey) {
        this.youtubeConfigured = youtubeKey != null && !youtubeKey.isBlank();
    }

    @GetMapping
    public List<Map<String,Object>> status() {
        return List.of(
            Map.of("provider","YOUTUBE","publicReadConfigured",youtubeConfigured,
                   "publishingEnabled",false,"requirement","Server-side YouTube Data API key"),
            Map.of("provider","FACEBOOK","publicReadConfigured",false,
                   "publishingEnabled",false,"requirement","Meta OAuth consent and API review"),
            Map.of("provider","INSTAGRAM","publicReadConfigured",false,
                   "publishingEnabled",false,"requirement","Instagram Graph API and permissions"),
            Map.of("provider","TIKTOK","publicReadConfigured",false,
                   "publishingEnabled",false,"requirement","TikTok developer app and OAuth consent")
        );
    }
}
