package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/campaigns")
public class CampaignWebController {
    private final CampaignOrderService service;
    public CampaignWebController(CampaignOrderService service){this.service=service;}
    @GetMapping
    public String list(Model model,Principal principal){
        model.addAttribute("orders",service.findMine(principal.getName()));
        return "campaign_list";
    }
    @GetMapping("/new")
    public String form(Model model){
        model.addAttribute("platforms",SocialPlatform.values());
        model.addAttribute("plans",CampaignServiceType.values());
        return "campaign_form";
    }
    @PostMapping
    public String create(@RequestParam SocialPlatform platform,@RequestParam CampaignServiceType serviceType,
                         @RequestParam String targetUrl,@RequestParam String brief,Principal principal,Model model){
        try {
            service.create(principal.getName(),new CampaignOrderRequest(platform,serviceType,targetUrl,brief));
            return "redirect:/campaigns";
        }catch(IllegalArgumentException ex){
            model.addAttribute("error",ex.getMessage());
            return form(model);
        }
    }
    @GetMapping("/{id}")
    public String details(@PathVariable Long id,Principal principal,Model model){
        model.addAttribute("order",service.findMineById(principal.getName(),id));
        model.addAttribute("events",service.historyForOwner(principal.getName(),id));
        return "campaign_details";
    }
}
