package com.example.fbservice.notification;

import java.security.Principal;
import java.time.LocalDate;
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
    private final NotificationSearch search;

    public NotificationController(AccountNotificationRepository notifications,NotificationSearch search) {
        this.notifications = notifications;
        this.search = search;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "false") boolean unreadOnly,
                       @RequestParam(defaultValue = "0") int days,
                       @RequestParam(defaultValue = "") String q,
                       Model model, Principal principal, HttpServletResponse response) {
        if (page < 0 || page > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification page");
        }
        // 0 means the entire history; limited fixed windows keep the UI predictable.
        if (days != 0 && days != 7 && days != 30 && days != 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification period");
        }
        String term = search.validate(q);
        String owner = principal.getName();
        PageRequest request = PageRequest.of(page, PAGE_SIZE,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<AccountNotification> result;
        if (!term.isBlank()) {
            LocalDateTime since = days == 0 ? null :
                    LocalDate.now().minusDays(days - 1L).atStartOfDay();
            result = search.search(owner, unreadOnly, since, term, request);
        } else if (days == 0) {
            result = unreadOnly
                    ? notifications.findByOwnerUsernameAndReadAtIsNull(owner, request)
                    : notifications.findByOwnerUsername(owner, request);
        } else {
            LocalDateTime since = LocalDate.now().minusDays(days - 1L).atStartOfDay();
            result = unreadOnly
                    ? notifications.findByOwnerUsernameAndReadAtIsNullAndCreatedAtGreaterThanEqual(
                            owner, since, request)
                    : notifications.findByOwnerUsernameAndCreatedAtGreaterThanEqual(owner, since, request);
        }

        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("items", result.getContent());
        model.addAttribute("unread", notifications.countByOwnerUsernameAndReadAtIsNull(owner));
        model.addAttribute("unreadOnly", unreadOnly);
        model.addAttribute("days", days);
        model.addAttribute("q", term);
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
