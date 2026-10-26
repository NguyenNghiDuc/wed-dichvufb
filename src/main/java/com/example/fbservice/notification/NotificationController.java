package com.example.fbservice.notification;

import java.security.Principal;
import java.time.LocalDateTime;
import org.springframework.data.domain.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="false") boolean unreadOnly,Model model,Principal principal) {
        if(page<0||page>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var paging=PageRequest.of(page,20,Sort.by(Sort.Direction.DESC,"createdAt","id"));
        var records=unreadOnly?notifications.findByOwnerUsernameAndReadAtIsNull(principal.getName(),paging)
            :notifications.findByOwnerUsername(principal.getName(),paging);
        model.addAttribute("records",records);
        model.addAttribute("items",records.getContent());
        model.addAttribute("unreadOnly",unreadOnly);
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
    @PostMapping("/read-all")
    @Transactional
    public String readAll(Principal principal,RedirectAttributes redirect){
        int count=notifications.markAllRead(principal.getName(),LocalDateTime.now());
        redirect.addFlashAttribute("success","Đã đánh dấu "+count+" thông báo là đã đọc.");
        return "redirect:/notifications";
    }
}
