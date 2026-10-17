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

/** Only an authenticated database account can rotate its own password. */
@Controller
@RequestMapping("/account/password")
public class PasswordChangeController {
    private final PasswordChangeService passwords;

    public PasswordChangeController(PasswordChangeService passwords) {
        this.passwords = passwords;
    }

    @GetMapping
    public String form() {
        return "account_password";
    }

    @PostMapping
    public String change(@RequestParam(required = false) String currentPassword,
                         @RequestParam(required = false) String newPassword,
                         @RequestParam(required = false) String confirmPassword,
                         Authentication authentication, HttpServletRequest request, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            passwords.change(principal, currentPassword, newPassword, confirmPassword);
        } catch (IllegalArgumentException invalid) {
            model.addAttribute("error", invalid.getMessage());
            return "account_password";
        }

        // Password rotation also logs out the current browser; all other sessions
        // are denied by AccountStatusFilter's password-revision comparison.
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/login?passwordChanged";
    }
}
