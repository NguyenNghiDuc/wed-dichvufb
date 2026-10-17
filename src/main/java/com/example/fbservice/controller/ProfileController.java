package com.example.fbservice.controller;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/** Read-only profile: no email change until ownership verification is implemented. */
@Controller
public class ProfileController {
    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User account = users.findByUsername(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("username", account.getUsername());
        model.addAttribute("email", account.getEmail());
        model.addAttribute("role", account.getRole());
        model.addAttribute("staffAccess", "STAFF".equals(account.getRole()) || "ADMIN".equals(account.getRole()));
        return "profile";
    }
}
