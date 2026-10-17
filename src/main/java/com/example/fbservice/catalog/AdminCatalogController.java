package com.example.fbservice.catalog;

import com.example.fbservice.campaign.CampaignServiceType;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/catalog")
public class AdminCatalogController {
    private final ServiceCatalogService catalog;
    public AdminCatalogController(ServiceCatalogService catalog){this.catalog=catalog;}

    @GetMapping
    public String list(Model model){
        model.addAttribute("services",catalog.all());
        model.addAttribute("events",catalog.recent());
        return "admin_catalog";
    }

    @PostMapping("/{type}")
    public String update(@PathVariable CampaignServiceType type,
                         @RequestParam long price,
                         @RequestParam(defaultValue="false") boolean enabled,
                         Principal principal,Model model){
        try {
            catalog.update(principal.getName(),type,price,enabled);
            return "redirect:/admin/catalog";
        }catch(IllegalArgumentException ex){
            model.addAttribute("error",ex.getMessage());
            return list(model);
        }
    }
}
