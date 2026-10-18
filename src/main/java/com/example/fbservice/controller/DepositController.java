package com.example.fbservice.controller;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.service.DepositService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/deposit")
public class DepositController {
    private final DepositService deposits;

    public DepositController(DepositService deposits) {
        this.deposits = deposits;
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("deposit", new DepositRequest());
        return "deposit_form";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("deposit") DepositRequest input, Principal principal, Model model) {
        try {
            deposits.save(input, principal.getName());
            return "redirect:/deposit/list";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "deposit_form";
        }
    }

    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        model.addAttribute("deposits", deposits.findAllForUser(principal.getName()));
        return "deposit_list";
    }
}
