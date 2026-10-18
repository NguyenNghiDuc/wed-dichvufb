package com.example.fbservice.admin;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdminStaffPerformanceController {
    private final AdminStaffPerformanceService service;
    public AdminStaffPerformanceController(AdminStaffPerformanceService service) {
        this.service=service;
    }

    @GetMapping("/admin/staff/performance")
    public String page(Model model) {
        model.addAttribute("performance",service.lastThirtyDays());
        return "admin_staff_performance";
    }

    @GetMapping("/api/v1/admin/staff/performance")
    @ResponseBody
    public ResponseEntity<java.util.List<AdminStaffPerformanceService.StaffPerformance>> api() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.lastThirtyDays());
    }
}
