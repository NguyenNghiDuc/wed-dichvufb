package com.example.fbservice.controller;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.service.ChatService;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Server-rendered chat: all identities are derived from the authenticated session. */
@Controller
@RequestMapping("/chat/ui")
public class ChatWebController {
    private final ChatService chats;
    private final UserRepository users;

    public ChatWebController(ChatService chats, UserRepository users) {
        this.chats = chats;
        this.users = users;
    }

    @GetMapping
    public String contacts(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) String q,
                           Model model, Principal principal) {
        Page<String> conversations;
        try {
            conversations = chats.contactNamesFor(principal.getName(), q, page);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bộ lọc hội thoại không hợp lệ");
        }
        model.addAttribute("staff", users.findTop100ByEnabledTrueAndRoleInOrderByUsernameAsc(
                List.of("STAFF")));
        model.addAttribute("conversations", conversations.getContent());
        model.addAttribute("unreadCounts",
            chats.unreadCountsForContacts(principal.getName(), conversations.getContent()));
        model.addAttribute("query", q);
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasPrevious", conversations.hasPrevious());
        model.addAttribute("hasNext", conversations.hasNext());
        return "chat_contacts";
    }

    @GetMapping("/open")
    public String open(@RequestParam String contact, Principal principal) {
        validateContact(principal.getName(), contact);
        return "redirect:/chat/ui/" + contact;
    }

    @GetMapping("/{other}")
    public String thread(@PathVariable String other, @RequestParam(defaultValue = "0") int page,
                         Principal principal, Model model) {
        validateContact(principal.getName(), other);
        Page<ChatMessage> records;
        try {
            records = chats.threadFor(principal.getName(), other, page, 20);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang hội thoại không hợp lệ");
        }
        model.addAttribute("other", other);
        model.addAttribute("messages", records.getContent());
        model.addAttribute("pageNumber", page);
        model.addAttribute("hasNext", records.hasNext());
        model.addAttribute("hasPrevious", records.hasPrevious());
        model.addAttribute("viewer", principal.getName());
        return "chat_thread";
    }

    @PostMapping("/{other}/send")
    public String send(@PathVariable String other, @RequestParam String content, Principal principal) {
        validateContact(principal.getName(), other);
        if (content == null || content.isBlank() || content.length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung tin nhắn không hợp lệ");
        chats.saveMessage(new ChatMessage(principal.getName(), other, content.strip()));
        return "redirect:/chat/ui/" + other;
    }

    /** One-message read receipt with the contact tied to this chat view. */
    @PostMapping("/{other}/messages/{id}/read")
    public String readOne(@PathVariable String other, @PathVariable Long id,
                          Principal principal) {
        validateContact(principal.getName(), other);
        try {
            chats.markMessageReadFrom(principal.getName(), other, id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin nhắn không hợp lệ");
        }
        return "redirect:/chat/ui/" + other;
    }

    @PostMapping("/{other}/read")
    public String read(@PathVariable String other, Principal principal) {
        validateContact(principal.getName(), other);
        chats.markThreadRead(principal.getName(), other);
        return "redirect:/chat/ui/" + other;
    }

    private void validateContact(String viewer, String other) {
        if (other == null || !other.matches("[a-zA-Z0-9._-]{3,40}") ||
                other.equals(viewer))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản liên hệ không hợp lệ");
        if (!users.existsByUsername(other))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
