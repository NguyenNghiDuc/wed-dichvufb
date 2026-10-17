package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/campaigns")
public class CampaignWebController {
    private final CampaignOrderService service;
    private final com.example.fbservice.catalog.ServiceCatalogService catalog;
    public CampaignWebController(CampaignOrderService service,
        com.example.fbservice.catalog.ServiceCatalogService catalog) {
        this.service=service;this.catalog=catalog;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model, Principal principal) {
        Page<CampaignOrder> records;
        try {
            records = service.pageMine(principal.getName(), page, 20);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        model.addAttribute("orders", records.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("totalPages", records.getTotalPages());
        return "campaign_list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("platforms", SocialPlatform.values());
        model.addAttribute("plans", catalog.available());
        return "campaign_form";
    }

    @PostMapping
    public String create(@RequestParam SocialPlatform platform, @RequestParam CampaignServiceType serviceType,
                         @RequestParam String targetUrl, @RequestParam String brief,
                         Principal principal, Model model) {
        try {
            service.create(principal.getName(), new CampaignOrderRequest(platform, serviceType, targetUrl, brief));
            return "redirect:/campaigns";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return form(model);
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, Model model) {
        try {
            service.cancelForOwner(principal.getName(), id);
            return "redirect:/campaigns";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return list(0, model, principal);
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("order", service.findMineById(principal.getName(), id));
        model.addAttribute("events", service.historyForOwner(principal.getName(), id));
        return "campaign_details";
    }
}
