package com.example.fbservice.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {
    private final AdminDashboardService dashboard;
    public AdminDashboardController(AdminDashboardService dashboard){this.dashboard=dashboard;}

    @GetMapping({"/admin","/admin/dashboard"})
    public String dashboard(Model model){
        model.addAttribute("metrics",dashboard.metrics());
        model.addAttribute("latest",dashboard.latestCampaigns());
        model.addAttribute("operations",dashboard.operations());
        return "admin_dashboard";
    }
}
