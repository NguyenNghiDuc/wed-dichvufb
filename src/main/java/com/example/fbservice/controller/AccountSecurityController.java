package com.example.fbservice.controller;

import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.security.SecurityActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/** Dedicated W1 page; no edits to W2-owned profile views. */
@Controller
public class AccountSecurityController {
    private final SecurityActivityService history;

    public AccountSecurityController(SecurityActivityService history) {
        this.history=history;
    }

    @GetMapping("/account/security")
    public String security(Authentication authentication, Model model) {
        if(authentication==null || !(authentication.getPrincipal() instanceof AccountPrincipal principal))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        model.addAttribute("events",history.recent(principal));
        return "account_security";
    }
}
