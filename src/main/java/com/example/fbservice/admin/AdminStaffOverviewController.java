package com.example.fbservice.admin;

import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** /staff/** already requires STAFF or ADMIN in the existing security configuration. */
@Controller
public class AdminStaffOverviewController {
    private final AdminStaffOverviewService overview;
    public AdminStaffOverviewController(AdminStaffOverviewService overview) {
        this.overview=overview;
    }

    @GetMapping("/staff/overview")
    public String page(Principal principal, Model model) {
        model.addAttribute("overview", overview.mine(principal.getName()));
        return "staff_overview";
    }

    @GetMapping("/staff/overview.json")
    @ResponseBody
    public ResponseEntity<AdminStaffOverviewService.Overview> json(Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(overview.mine(principal.getName()));
    }
}
