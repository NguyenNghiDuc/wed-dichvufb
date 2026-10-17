package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/staff/campaign-orders")
public class StaffCampaignController {
    private final CampaignAssignmentService assignments;

    public StaffCampaignController(CampaignAssignmentService assignments) {
        this.assignments = assignments;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        Page<CampaignOrder> records = assignments.assignedTo(principal.getName(), page);
        model.addAttribute("orders", records.getContent());
        model.addAttribute("page", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        return "staff_campaigns";
    }

    @PostMapping("/{id}/progress")
    public String progress(@PathVariable Long id, @RequestParam CampaignStatus status,
            Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            assignments.progress(id, status, principal.getName());
            flash.addFlashAttribute("notice", "Đã cập nhật tiến độ.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/staff/campaign-orders";
    }
}
