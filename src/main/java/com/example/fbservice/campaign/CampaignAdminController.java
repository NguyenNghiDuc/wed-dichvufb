package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/campaign-orders")
public class CampaignAdminController {
    private final CampaignOrderService service;
    public CampaignAdminController(CampaignOrderService service){this.service=service;}
    @GetMapping
    public String list(Model model){
        model.addAttribute("orders",service.findAllForAdmin());
        model.addAttribute("statuses",CampaignStatus.values());
        return "admin_campaigns";
    }
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,@RequestParam CampaignStatus status,Principal principal,Model model){
        try{
            service.updateStatusForAdmin(id,status,principal.getName());
            return "redirect:/admin/campaign-orders";
        }catch(IllegalArgumentException ex){
            model.addAttribute("error",ex.getMessage());
            return list(model);
        }
    }
}
