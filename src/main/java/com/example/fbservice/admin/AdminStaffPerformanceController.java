package com.example.fbservice.admin;

import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String page(@RequestParam(required=false) String actor, Model model) {
        model.addAttribute("performance",service.filtered(actor));
        model.addAttribute("selectedActor",actor == null ? "" : actor.trim());
        return "admin_staff_performance";
    }

    @GetMapping("/api/v1/admin/staff/performance")
    @ResponseBody
    public ResponseEntity<java.util.List<AdminStaffPerformanceService.StaffPerformance>> api(
            @RequestParam(required=false) String actor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.filtered(actor));
    }
}
