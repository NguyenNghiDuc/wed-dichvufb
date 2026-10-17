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
                         @RequestParam(required=false) Long expectedVersion,
                         Principal principal,Model model){
        try {
            catalog.update(principal.getName(),type,price,enabled,expectedVersion);
            return "redirect:/admin/catalog";
        }catch(IllegalArgumentException ex){
            model.addAttribute("error",ex.getMessage());
            return list(model);
        }catch(org.springframework.orm.ObjectOptimisticLockingFailureException ex){
            model.addAttribute("error","Bảng giá đã có thay đổi đồng thời. Tải lại trước khi lưu.");
            return list(model);
        }
    }
}
