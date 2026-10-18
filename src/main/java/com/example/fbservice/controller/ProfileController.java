package com.example.fbservice.controller;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.service.AccountSettingsService;
import com.example.fbservice.config.TrafficLimiter;
import jakarta.servlet.http.*;
import java.security.Principal;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {
    private final UserRepository users;
    private final AccountSettingsService settings;
    private final TrafficLimiter limiter;

    public ProfileController(UserRepository users, AccountSettingsService settings, TrafficLimiter limiter) {
        this.users = users;
        this.settings = settings;
        this.limiter = limiter;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User account = users.findByUsername(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("username", account.getUsername());
        model.addAttribute("displayName", account.getDisplayName());
        model.addAttribute("email", account.getEmail());
        model.addAttribute("role", account.getRole());
        model.addAttribute("staffAccess", "STAFF".equals(account.getRole()) || "ADMIN".equals(account.getRole()));
        return "profile";
    }

    @PostMapping("/profile/display-name")
    public String displayName(@RequestParam(defaultValue = "") String displayName, Principal principal,
                              Model model, RedirectAttributes redirect) {
        try {
            settings.updateDisplayName(principal.getName(), displayName);
            redirect.addFlashAttribute("success", "Đã lưu tên hiển thị. Tên đăng nhập của bạn không thay đổi.");
            return "redirect:/profile";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return profile(principal, model);
        }
    }

    @PostMapping("/profile/password")
    public String password(@RequestParam String currentPassword, @RequestParam String newPassword,
                           @RequestParam String confirmation, Principal principal, Model model,
                           HttpServletRequest request, HttpServletResponse response) {
        limiter.check("settings-password:" + principal.getName(), 5, Duration.ofMinutes(15));
        try {
            settings.changePassword(principal.getName(), currentPassword, newPassword, confirmation);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return profile(principal, model);
        }
        logout(request, response);
        return "redirect:/login?passwordChanged";
    }

    @PostMapping("/profile/sessions/revoke")
    public String revoke(@RequestParam String currentPassword, Principal principal, Model model,
                         HttpServletRequest request, HttpServletResponse response) {
        limiter.check("settings-sessions:" + principal.getName(), 5, Duration.ofMinutes(15));
        try {
            settings.revokeSessions(principal.getName(), currentPassword);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return profile(principal, model);
        }
        logout(request, response);
        return "redirect:/login?sessionsClosed";
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response,
            SecurityContextHolder.getContext().getAuthentication());
    }
}
