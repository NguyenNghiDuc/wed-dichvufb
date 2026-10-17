package com.example.fbservice.campaign;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/campaign-orders")
public class CampaignAdminController {
    private final CampaignOrderService service;
    private final CampaignAssignmentService assignments;

    public CampaignAdminController(CampaignOrderService service,
                                   CampaignAssignmentService assignments) {
        this.service = service;
        this.assignments = assignments;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<CampaignOrder> records;
        try {
            records = service.pageForAdmin(page);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        model.addAttribute("orders", records.getContent());
        model.addAttribute("employees", assignments.eligibleStaff());
        model.addAttribute("statuses", CampaignStatus.values());
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("totalPages", records.getTotalPages());
        return "admin_campaigns";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam CampaignStatus status,
                               Principal principal, Model model) {
        try {
            service.updateStatusForAdmin(id, status, principal.getName());
            return "redirect:/admin/campaign-orders";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return list(0, model);
        }
    }
}
