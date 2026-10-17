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
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) Long orderId,
                       @RequestParam(required = false) CampaignStatus status,
                       Model model, Principal principal) {
        Page<CampaignOrder> records;
        try {
            records = service.pageMine(principal.getName(), page, 20, orderId, status);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filters or pagination");
        }
        // Aggregate across every page, including orders hidden by the current filter.
        model.addAttribute("summary", service.summaryForOwner(principal.getName()));
        model.addAttribute("orders", records.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("totalPages", records.getTotalPages());
        model.addAttribute("orderId", orderId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", CampaignStatus.values());
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
            return list(0, null, null, model, principal);
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                          Principal principal, Model model) {
        Page<CampaignHistoryEntry> history;
        try {
            history = service.pageHistoryForOwner(principal.getName(), id, page, 20);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid history page");
        }
        model.addAttribute("order", service.findMineById(principal.getName(), id));
        model.addAttribute("events", history.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("totalPages", history.getTotalPages());
        model.addAttribute("hasPrevious", history.hasPrevious());
        model.addAttribute("hasNext", history.hasNext());
        return "campaign_details";
    }
}
