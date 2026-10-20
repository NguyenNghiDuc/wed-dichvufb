package com.example.fbservice.controller;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.service.DepositService;
import com.example.fbservice.payment.WalletCreditRepository;
import org.springframework.beans.factory.annotation.Value;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/deposit")
public class DepositController {
    private final DepositService deposits;
    private final WalletCreditRepository credits;
    private final String bankAccount;
    private final String accountHolder;

    public DepositController(DepositService deposits, WalletCreditRepository credits,
            @Value("#{environment.getProperty('growth.payments.bank.account-number', '')}") String bankAccount,
            @Value("#{environment.getProperty('growth.payments.bank.account-holder', '')}") String accountHolder) {
        this.deposits = deposits;
        this.credits = credits;
        this.bankAccount = bankAccount.trim();
        this.accountHolder = accountHolder.trim();
    }

    private boolean bankReady() {
        return bankAccount.matches("[0-9]{6,24}") && !accountHolder.isBlank();
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("deposit", new DepositRequest());
        model.addAttribute("bankReady", bankReady());
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
            model.addAttribute("bankReady", bankReady());
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
        DepositRequest deposit=deposits.detail(id,principal.getName());
        model.addAttribute("deposit",deposit);
        model.addAttribute("bankReady",bankReady());
        model.addAttribute("bankAccount",bankAccount);
        model.addAttribute("accountHolder",accountHolder);
        model.addAttribute("transferReference","GLNAP"+deposit.getId());
        model.addAttribute("walletCredited",credits.existsByDepositRequestId(deposit.getId()));
        return "deposit_detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,Principal principal,RedirectAttributes redirect) {
        deposits.cancel(id,principal.getName());redirect.addFlashAttribute("success","Đã hủy yêu cầu nạp tiền.");
        return "redirect:/deposit/"+id;
    }

}
