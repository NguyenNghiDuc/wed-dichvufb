package com.example.fbservice.controller;

import com.example.fbservice.service.DepositService;
import com.example.fbservice.payment.WalletCreditRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.data.domain.PageRequest;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/admin/deposits")
public class AdminDepositController {
    private final DepositService deposits;
    private final WalletCreditRepository credits;
    private final DepositRequestRepository depositRepo;
    public AdminDepositController(DepositService deposits,WalletCreditRepository credits,DepositRequestRepository depositRepo){
        this.deposits=deposits;this.credits=credits;this.depositRepo=depositRepo;
    }
    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) String status,
                       @RequestParam(required=false) String owner,Model model){
        String username=owner==null||owner.isBlank()?null:owner.trim();
        if(username!=null&&username.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var records=deposits.page(username,status,page);
        model.addAttribute("records",records);
        var ids=records.getContent().stream().map(d->d.getId()).toList();
        model.addAttribute("creditedDepositIds",ids.isEmpty()?java.util.Set.of():
            credits.findByDepositRequestIdIn(ids).stream().map(c->c.getDepositRequestId())
                .collect(Collectors.toSet()));
        model.addAttribute("selectedStatus",status);model.addAttribute("owner",username);
        return "admin_deposits";
    }
    @GetMapping("/reconciliation")
    public String reconciliation(@RequestParam(defaultValue="0") int page,Model model){
        if(page<0||page>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        model.addAttribute("approvedWithoutCredit",depositRepo.countApprovedWithoutCredit());
        model.addAttribute("pendingCount",depositRepo.countByStatus("PENDING"));
        model.addAttribute("olderPendingCount",depositRepo.countByStatusAndCreatedAtBefore("PENDING",
            LocalDateTime.now(ZoneOffset.UTC).minusDays(1)));
        model.addAttribute("creditedCount",credits.count());
        model.addAttribute("creditedTotal",credits.totalCreditedVnd());
        model.addAttribute("uncredited",depositRepo.findApprovedWithoutCredit(PageRequest.of(page,20)));
        return "admin_payment_reconciliation";
    }
}
