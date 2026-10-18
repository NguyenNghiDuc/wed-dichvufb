package com.example.fbservice.admin;

import com.example.fbservice.campaign.CampaignServiceType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminCatalogHistoryController {
    private final AdminCatalogHistoryService service;
    public AdminCatalogHistoryController(AdminCatalogHistoryService service) {
        this.service = service;
    }

    @GetMapping("/admin/catalog/history")
    public String show(@RequestParam(defaultValue="0") int page,
                       @RequestParam(required=false) CampaignServiceType type,
                       @RequestParam(required=false) String actor, Model model) {
        var p=service.page(page,type,actor);
        model.addAttribute("events",p.events());
        model.addAttribute("page",page);
        model.addAttribute("hasNext",p.hasNext());
        model.addAttribute("hasPrevious",page>0);
        model.addAttribute("type",type);
        model.addAttribute("actor",actor == null ? "" : actor.trim());
        model.addAttribute("types",CampaignServiceType.values());
        return "admin_catalog_history";
    }
}
