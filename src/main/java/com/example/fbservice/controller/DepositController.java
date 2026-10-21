package com.example.fbservice.controller;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.service.DepositService;
import com.example.fbservice.payment.WalletCreditRepository;
import org.springframework.beans.factory.annotation.Value;
import java.security.Principal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final String cassoSecret;
    private final boolean cassoEnabled;
    private final boolean liveReady;

    public DepositController(DepositService deposits, WalletCreditRepository credits,
            @Value("#{environment.getProperty('growth.payments.bank.account-number', '')}") String bankAccount,
            @Value("#{environment.getProperty('growth.payments.bank.account-holder', '')}") String accountHolder,
            @Value("#{environment.getProperty('growth.payments.casso.webhook-secret', '')}") String cassoSecret,
            @Value("#{environment.getProperty('growth.payments.casso.enabled', 'false')}") boolean cassoEnabled,
            @Value("#{environment.getProperty('growth.payments.live-ready', 'false')}") boolean liveReady) {
        this.deposits = deposits;
        this.credits = credits;
        this.bankAccount = bankAccount.trim();
        this.accountHolder = accountHolder.trim();
        this.cassoSecret = cassoSecret;
        this.cassoEnabled = cassoEnabled;
        this.liveReady = liveReady;
    }

    private boolean bankReady() {
        return bankAccount.matches("[0-9]{6,24}") && !accountHolder.isBlank()
            && liveReady && cassoEnabled && cassoSecret != null && cassoSecret.length() >= 32;
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
        if(bankReady() && "PENDING".equals(deposit.getStatus())) {
            model.addAttribute("qrUrl","https://img.vietqr.io/image/970407-"+bankAccount
                +"-compact2.png?amount="+deposit.getAmount()
                +"&addInfo=GLNAP"+deposit.getId()
                +"&accountName="+URLEncoder.encode(accountHolder,StandardCharsets.UTF_8));
        }
        return "deposit_detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,Principal principal,RedirectAttributes redirect) {
        deposits.cancel(id,principal.getName());redirect.addFlashAttribute("success","Đã hủy yêu cầu nạp tiền.");
        return "redirect:/deposit/"+id;
    }

}
