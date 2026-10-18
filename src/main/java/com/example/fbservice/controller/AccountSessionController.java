package com.example.fbservice.controller;

import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.service.PasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * An authenticated account may revoke all of its existing session credentials.
 * Password is re-hashed with a new salt; the user's password does not change.
 */
@Controller
@RequestMapping("/account/sessions")
public class AccountSessionController {
    private final PasswordChangeService credentials;

    public AccountSessionController(PasswordChangeService credentials) {
        this.credentials = credentials;
    }

    @GetMapping
    public String form(HttpServletRequest request, Model model) {
        showCurrentConnection(request, model);
        return "account_sessions";
    }

    /**
     * Present only THIS request/session. Never trust user-provided forwarded IP
     * headers as a durable device identifier or persist the raw User-Agent.
     */
    private static void showCurrentConnection(HttpServletRequest request, Model model) {
        model.addAttribute("connectionAddress", safePreview(request.getRemoteAddr(), 80));
        model.addAttribute("browserDescription", safePreview(request.getHeader("User-Agent"), 160));
        HttpSession current = request.getSession(false);
        if (current != null) {
            model.addAttribute("sessionCreatedUtc",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(current.getCreationTime()), ZoneOffset.UTC));
            model.addAttribute("sessionLastAccessUtc",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(current.getLastAccessedTime()), ZoneOffset.UTC));
            model.addAttribute("sessionTimeoutMinutes",
                current.getMaxInactiveInterval() <= 0 ? 0 :
                    (current.getMaxInactiveInterval() + 59) / 60);
        }
    }

    private static String safePreview(String value, int codepointLimit) {
        if (value == null || value.isBlank()) return "Không xác định";
        StringBuilder preview = new StringBuilder();
        value.codePoints().filter(cp -> !Character.isISOControl(cp))
            .limit(codepointLimit).forEach(preview::appendCodePoint);
        return preview.isEmpty() ? "Không xác định" : preview.toString();
    }

    @PostMapping("/revoke")
    public String revoke(@RequestParam(required=false) String currentPassword,
                         Authentication authentication, HttpServletRequest request, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            credentials.revokeAll(principal, currentPassword);
        } catch (IllegalArgumentException invalid) {
            model.addAttribute("error", invalid.getMessage());
            showCurrentConnection(request, model);
            return "account_sessions";
        }

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/login?sessionsRevoked";
    }
}
