package com.example.fbservice.controller;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.service.DepositService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String create(@ModelAttribute("deposit") DepositRequest input, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            deposits.save(input, principal.getName());
            redirect.addFlashAttribute("success", "Đã gửi yêu cầu nạp tiền. Yêu cầu đang chờ xử lý, chưa ghi có số dư.");
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
