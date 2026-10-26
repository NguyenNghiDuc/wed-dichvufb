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
    public String list(Model model, Principal principal,@RequestParam(defaultValue="0") int page,
                       @RequestParam(required=false) String status) {
        var records=deposits.page(principal.getName(),status,page);
        model.addAttribute("deposits",records.getContent());
        model.addAttribute("records",records);model.addAttribute("selectedStatus",status);
        return "deposit_list";
    }
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,Principal principal,Model model) {
        model.addAttribute("deposit",deposits.detail(id,principal.getName()));return "deposit_detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,Principal principal,RedirectAttributes redirect) {
        deposits.cancel(id,principal.getName());redirect.addFlashAttribute("success","Đã hủy yêu cầu nạp tiền.");
        return "redirect:/deposit/"+id;
    }

}
