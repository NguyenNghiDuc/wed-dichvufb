package com.example.fbservice.notification;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    private final AccountNotificationRepository notifications;
    public NotificationController(AccountNotificationRepository notifications) {
        this.notifications=notifications;
    }
    @GetMapping
    public String list(Model model,Principal principal) {
        model.addAttribute("items",notifications.findTop100ByOwnerUsernameOrderByCreatedAtDesc(principal.getName()));
        model.addAttribute("unread",notifications.countByOwnerUsernameAndReadAtIsNull(principal.getName()));
        return "notifications";
    }
    @PostMapping("/{id}/read")
    @Transactional
    public String markRead(@PathVariable Long id,Principal principal) {
        var notification=notifications.findByIdAndOwnerUsername(id,principal.getName())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        notification.markRead();
        return "redirect:/notifications";
    }
}
