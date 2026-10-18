package com.example.fbservice.support;

import java.security.Principal;
import org.springframework.data.domain.Page;
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
                       @RequestParam(required=false) String q,@RequestParam(required=false) Long ticketId,
                       @RequestParam(required=false) TicketStatus status,@RequestParam(required=false) String owner,Model model) {
        Page<SupportTicket> records = service.searchForAdmin(page,q,ticketId,status,owner);
        model.addAttribute("filterQuery",com.example.fbservice.web.SearchText.clean(q,120));
        model.addAttribute("ticketId",ticketId);model.addAttribute("selectedStatus",status);
        model.addAttribute("ownerFilter",com.example.fbservice.web.SearchText.clean(owner,100));
        model.addAttribute("ticketStatuses",TicketStatus.values());
        model.addAttribute("totalPages",records.getTotalPages());model.addAttribute("totalElements",records.getTotalElements());
        model.addAttribute("tickets", records.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("hasNext", records.hasNext());
        return "admin_support_list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("ticket", service.detailForAdmin(id));
        model.addAttribute("messages", service.messagesForAdmin(id));
        return "admin_support_detail";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String message,
                        Principal principal, Model model) {
        try {
            service.replyAsAdmin(principal.getName(), id, message);
            return "redirect:/admin/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("inputMessage", message);
            return details(id, model);
        }
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, Principal principal, Model model) {
        try {
            service.closeAsAdmin(principal.getName(), id);
            return "redirect:/admin/support/" + id;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            return details(id, model);
        }
    }
}
