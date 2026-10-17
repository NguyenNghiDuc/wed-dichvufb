package com.example.fbservice.controller;

import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.service.PasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    public String form() {
        return "account_sessions";
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
            return "account_sessions";
        }

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/login?sessionsRevoked";
    }
}
