package com.example.fbservice.support;

import java.security.Principal;
import org.springframework.data.domain.Page;
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
    public String list(@RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        Page<SupportTicket> records = service.listMine(principal.getName(), page);
        model.addAttribute("tickets", records.getContent());
        model.addAttribute("pageNumber", page);
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

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("ticket", service.detailForOwner(principal.getName(), id));
        model.addAttribute("messages", service.messagesForOwner(principal.getName(), id));
        return "support_detail";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String message,
                        Principal principal, Model model) {
        try {
            service.replyAsOwner(principal.getName(), id, message);
            return "redirect:/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, principal, model);
        }
    }
}
