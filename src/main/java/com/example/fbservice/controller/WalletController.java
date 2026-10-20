package com.example.fbservice.controller;
import com.example.fbservice.payment.WalletCreditRepository;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WalletController {
    private final WalletCreditRepository credits;
    public WalletController(WalletCreditRepository credits){this.credits=credits;}
    @GetMapping("/wallet")
    public String wallet(Principal principal,Model model){
        String owner=principal.getName();
        model.addAttribute("balanceVnd",credits.balanceForUser(owner));
        model.addAttribute("credits",credits.findTop50ByOwnerUsernameOrderByCreatedAtDescIdDesc(owner));
        return "wallet";
    }
}
