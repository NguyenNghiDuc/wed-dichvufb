package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/campaign-orders")
public class CampaignAssignmentAdminController {
    private final CampaignAssignmentService assignments;
    private final CampaignOrderRepository orders;

    public CampaignAssignmentAdminController(CampaignAssignmentService assignments,
                                             CampaignOrderRepository orders) {
        this.assignments = assignments;
        this.orders = orders;
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam(required = false) Long employeeId,
                         Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            assignments.assign(id, employeeId, principal.getName());
            flash.addFlashAttribute("notice", "Đã lưu phân công.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/campaign-orders";
    }

    @GetMapping("/{id}/assignment-history")
    public String history(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("order", orders.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND)));
        model.addAttribute("events", assignments.assignmentHistory(principal.getName(), id));
        return "admin_assignment_history";
    }
}
