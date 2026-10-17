package com.example.fbservice.notification;

import java.security.Principal;
import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    private static final int PAGE_SIZE = 20;
    private final AccountNotificationRepository notifications;

    public NotificationController(AccountNotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean unreadOnly,
                       Model model, Principal principal, HttpServletResponse response) {
        if (page < 0 || page > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification page");
        }
        String owner = principal.getName();
        PageRequest request = PageRequest.of(page, PAGE_SIZE,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<AccountNotification> result = unreadOnly
                ? notifications.findByOwnerUsernameAndReadAtIsNull(owner, request)
                : notifications.findByOwnerUsername(owner, request);

        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("items", result.getContent());
        model.addAttribute("unread", notifications.countByOwnerUsernameAndReadAtIsNull(owner));
        model.addAttribute("unreadOnly", unreadOnly);
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", result.hasPrevious());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("totalPages", result.getTotalPages());
        return "notifications";
    }

    @PostMapping("/{id}/read")
    @Transactional
    public String markRead(@PathVariable Long id, Principal principal) {
        var notification = notifications.findByIdAndOwnerUsername(id, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        notification.markRead();
        return "redirect:/notifications";
    }

    /** Idempotent; only the logged-in customer's unread rows are changed. */
    @PostMapping("/read-all")
    @Transactional
    public String markAllRead(Principal principal) {
        notifications.markAllUnreadAsRead(principal.getName(), LocalDateTime.now());
        return "redirect:/notifications";
    }
}
