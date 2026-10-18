package com.example.fbservice.support;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/support")
public class SupportController {
    private final SupportTicketService service;

    public SupportController(SupportTicketService service) { this.service = service; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String subject,
                       @RequestParam(required = false) TicketStatus status,
                       Principal principal, Model model) {
        Page<SupportTicket> records = service.searchMine(principal.getName(), page, subject, status);
        model.addAttribute("tickets", records.getContent());
        model.addAttribute("ticketCounts", service.countsForOwner(principal.getName()));
        model.addAttribute("pageNumber", page);
        model.addAttribute("subject", subject);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("hasPrevious", records.hasPrevious());
        return "support_list";
    }

    @GetMapping("/new")
    public String form() { return "support_form"; }

    @PostMapping
    public String create(@RequestParam String subject, @RequestParam String description,
                         Principal principal, Model model) {
        try {
            SupportTicket ticket = service.create(principal.getName(), subject, description);
            return "redirect:/support/" + ticket.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return "support_form";
        }
    }

    /** Customer mobile/API summary; counts are always scoped to the signed-in owner. */
    @GetMapping("/stats")
    public ResponseEntity<SupportTicketCounts> stats(Principal principal) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(service.countsForOwner(principal.getName()));
    }

    /** Customer-specific ticket reply page for mobile clients. */
    @GetMapping("/{id}/messages")
    public org.springframework.http.ResponseEntity<Page<SupportMessageEntry>> messages(
            @PathVariable Long id, @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q, Principal principal) {
        Page<SupportMessageEntry> result = service
            .searchMessagesForOwner(principal.getName(), id, page, q)
            .map(SupportMessageEntry::from);
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store")
            .body(result);
    }

    /** Owner-only progress data; does not enumerate other customers' tickets. */
    @GetMapping("/{id}/progress")
    public ResponseEntity<SupportTicketProgress> progress(@PathVariable Long id,
                                                          Principal principal) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(service.progressForOwner(principal.getName(), id));
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) String q,
                          Principal principal, Model model) {
        var ticket = service.detailForOwner(principal.getName(), id);
        var messages = service.searchMessagesForOwner(principal.getName(), id, page, q);
        model.addAttribute("ticket", ticket);
        model.addAttribute("progress", service.progressForOwner(principal.getName(), id));
        model.addAttribute("messages", messages.getContent());
        model.addAttribute("query", q);
        model.addAttribute("messagePage", page);
        model.addAttribute("hasPrevious", messages.hasPrevious());
        model.addAttribute("hasNext", messages.hasNext());
        return "support_detail";
    }

    /**
     * Owner-only reopening. The service locks a ticket with both its ID
     * and authenticated owner; no user-supplied owner can override it.
     */
    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id, @RequestParam String reason,
                         Principal principal, Model model) {
        try {
            service.reopenAsOwner(principal.getName(), id, reason);
            return "redirect:/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, 0, null, principal, model);
        }
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String message,
                        Principal principal, Model model) {
        try {
            service.replyAsOwner(principal.getName(), id, message);
            return "redirect:/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, 0, null, principal, model);
        }
    }
}
