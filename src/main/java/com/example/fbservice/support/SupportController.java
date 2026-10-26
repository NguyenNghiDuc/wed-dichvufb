package com.example.fbservice.support;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/support")
public class SupportController {
    private final SupportTicketService service;

    public SupportController(SupportTicketService service) { this.service = service; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required=false) String q,@RequestParam(required=false) Long ticketId,
                       @RequestParam(required=false) TicketStatus status,Principal principal,Model model) {
        Page<SupportTicket> records = service.searchMine(principal.getName(),page,q,ticketId,status);
        model.addAttribute("filterQuery",com.example.fbservice.web.SearchText.clean(q,120));
        model.addAttribute("ticketId",ticketId);model.addAttribute("selectedStatus",status);
        model.addAttribute("ticketStatuses",TicketStatus.values());
        model.addAttribute("totalPages",records.getTotalPages());model.addAttribute("totalElements",records.getTotalElements());
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
                         Principal principal, Model model, RedirectAttributes redirect) {
        try {
            SupportTicket ticket = service.create(principal.getName(), subject, description);
            redirect.addFlashAttribute("success", "Đã gửi yêu cầu hỗ trợ.");
            return "redirect:/support/" + ticket.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("inputSubject", subject);
            model.addAttribute("inputDescription", description);
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
            model.addAttribute("inputMessage", message);
            return details(id, principal, model);
        }
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        service.closeAsOwner(principal.getName(), id);
        redirect.addFlashAttribute("success", "Yêu cầu hỗ trợ đã đóng.");
        return "redirect:/support/" + id;
    }
    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id,@RequestParam String reason,Principal principal,Model model,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            service.reopenAsOwner(principal.getName(),id,reason);
            redirect.addFlashAttribute("success","Đã mở lại yêu cầu hỗ trợ.");
            return "redirect:/support/"+id;
        } catch(IllegalArgumentException exception) {
            model.addAttribute("error",exception.getMessage());model.addAttribute("inputReason",reason);
            return details(id, principal, model);
        }
    }

}
