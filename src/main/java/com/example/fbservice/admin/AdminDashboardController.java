package com.example.fbservice.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminDashboardController {
    private final AdminDashboardService dashboard;
    private final AdminOrderTrendService trends;
    public AdminDashboardController(AdminDashboardService dashboard, AdminOrderTrendService trends){
        this.dashboard=dashboard;this.trends=trends;
    }

    @GetMapping({"/admin","/admin/dashboard"})
    public String dashboard(@RequestParam(defaultValue = "DAY") AdminOrderTrendService.Period period,
                            Model model){
        model.addAttribute("metrics",dashboard.metrics());
        model.addAttribute("latest",dashboard.latestCampaigns());
        model.addAttribute("operations",dashboard.operations());
        model.addAttribute("userRoles",dashboard.userRoleBreakdown());
        model.addAttribute("trend",trends.find(period));
        model.addAttribute("period",period);
        return "admin_dashboard";
    }
}
