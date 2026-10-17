package com.example.fbservice.content;

import com.example.fbservice.config.TrafficLimiter;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Duration;
import org.springframework.web.bind.annotation.*;

/** Authenticated, CSRF-protected, read-only calculations; no provider calls or persistence. */
@RestController
@RequestMapping("/api/v1/content-analytics")
public class ContentMetricsController {
    private final ContentMetricsService metrics;
    private final TrafficLimiter limiter;
    public ContentMetricsController(ContentMetricsService metrics,TrafficLimiter limiter) {
        this.metrics=metrics;
        this.limiter=limiter;
    }

    @PostMapping("/preview")
    public ContentMetricsService.Summary preview(@Valid @RequestBody ContentMetricsRequest data,
                                                  Principal principal) {
        limiter.check("content-metrics:"+principal.getName(),30,Duration.ofMinutes(1));
        return metrics.analyze(data);
    }
}
