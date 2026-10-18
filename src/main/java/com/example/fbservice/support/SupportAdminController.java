package com.example.fbservice.support;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/support")
public class SupportAdminController {
    private final SupportTicketService service;
    public SupportAdminController(SupportTicketService service) { this.service = service; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String subject,
                       @RequestParam(required = false) TicketStatus status,
                       @RequestParam(required = false) TicketPriority priority,
                       @RequestParam(defaultValue = "ALL") TicketAssignmentFilter assignment,
                       @RequestParam(required = false) String customer,
                       Model model, Principal principal) {
        Page<SupportTicket> records = service.searchForStaff(
            page, subject, status, priority, principal.getName(), assignment, customer);
        model.addAttribute("tickets", records.getContent());
        model.addAttribute("ticketCounts", service.countsForStaff());
        model.addAttribute("pageNumber", page);
        model.addAttribute("subject", subject);
        model.addAttribute("customer", customer);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedAssignment", assignment);
        model.addAttribute("assignments", TicketAssignmentFilter.values());
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", TicketPriority.values());
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        return "admin_support_list";
    }

    /** Staff-only queue summary, protected by the existing /admin/support role gate. */
    @GetMapping("/stats")
    public ResponseEntity<SupportTicketCounts> stats() {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(service.countsForStaff());
    }

    /** Restricted to the existing STAFF/ADMIN route authorization. */
    @GetMapping("/{id}/messages")
    public org.springframework.http.ResponseEntity<Page<SupportMessageEntry>> messages(
            @PathVariable Long id, @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q) {
        Page<SupportMessageEntry> result = service.searchMessagesForStaff(id, page, q)
            .map(SupportMessageEntry::from);
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
            .body(result);
    }

    /** STAFF/ADMIN operational progress, protected by the existing route gate. */
    @GetMapping("/{id}/progress")
    public ResponseEntity<SupportTicketProgress> progress(@PathVariable Long id) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(service.progressForStaff(id));
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) String q,
                          Model model, Principal principal) {
        var ticket = service.detailForAdmin(id);
        var messages = service.searchMessagesForStaff(id, page, q);
        model.addAttribute("ticket", ticket);
        model.addAttribute("progress", service.progressForStaff(id));
        boolean admin = service.isActiveAdmin(principal.getName());
        model.addAttribute("canReassign", admin);
        if (admin) model.addAttribute("assignees", service.eligibleAssignees());
        model.addAttribute("assignmentHistory", service.assignmentHistory(id));
        model.addAttribute("messages", messages.getContent());
        model.addAttribute("query", q);
        model.addAttribute("messagePage", page);
        model.addAttribute("hasPrevious", messages.hasPrevious());
        model.addAttribute("hasNext", messages.hasNext());
        return "admin_support_detail";
    }

    @PostMapping("/{id}/claim")
    public String claim(@PathVariable Long id, Principal principal, Model model) {
        try {
            service.claimAsStaff(principal.getName(), id);
            return "redirect:/admin/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, 0, null, model, principal);
        }
    }

    @PostMapping("/{id}/reassign")
    public String reassign(@PathVariable Long id,
                           @RequestParam(required = false) Long employeeId,
                           Principal principal, Model model) {
        try {
            service.reassignAsAdmin(principal.getName(), id, employeeId);
            return "redirect:/admin/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, 0, null, model, principal);
        }
    }

    @PostMapping("/{id}/priority")
    public String prioritize(@PathVariable Long id, @RequestParam TicketPriority priority,
                             Model model, Principal principal) {
        try {
            service.prioritizeAsStaff(id, priority);
            return "redirect:/admin/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, 0, null, model, principal);
        }
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String message,
                        Principal principal, Model model) {
        try {
            service.replyAsAdmin(principal.getName(), id, message);
            return "redirect:/admin/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, 0, null, model, principal);
        }
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, Principal principal, Model model) {
        try {
            service.closeAsAdmin(principal.getName(), id);
            return "redirect:/admin/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, 0, null, model, principal);
        }
    }
}
