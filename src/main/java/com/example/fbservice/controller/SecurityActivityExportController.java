package com.example.fbservice.controller;

import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.security.SecurityActivityService;
import java.time.format.DateTimeFormatter;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/** Bounded, owner-scoped CSV export with no uncontrolled user fields. */
@Controller
public class SecurityActivityExportController {
    private final SecurityActivityService history;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    public SecurityActivityExportController(SecurityActivityService history) {
        this.history = history;
    }

    @GetMapping("/account/security/export")
    public ResponseEntity<String> export(Authentication authentication) {
        if (authentication==null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        StringBuilder csv=new StringBuilder("time_utc,event_kind\r\n");
        for (var activity:history.exportRecent(principal)) {
            // Both fields are server-generated (UTC datetime and fixed enum), so
            // CSV injection from user-supplied values is not possible.
            csv.append(ISO.format(activity.getCreatedAt())).append(',')
               .append(activity.getKind().name()).append("\r\n");
        }
        return ResponseEntity.ok()
            .contentType(new MediaType("text","csv",java.nio.charset.StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"growlab-security-activity.csv\"")
            .header(HttpHeaders.CACHE_CONTROL,"no-store")
            .body(csv.toString());
    }
}
