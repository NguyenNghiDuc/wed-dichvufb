package com.example.fbservice.controller;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import com.example.fbservice.repository.UserRepository;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chat;
    private final UserRepository users;

    public ChatController(ChatService chat, UserRepository users) {
        this.chat = chat;
        this.users = users;
    }

    @PostMapping("/send")
    public ChatMessage sendMessage(@RequestBody ChatMessage message, Principal principal) {
        if (message == null || message.getReceiver() == null || message.getContent() == null
                || message.getContent().isBlank() || message.getContent().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message");
        }
        String receiver = message.getReceiver().trim();
        if (!receiver.matches("[a-zA-Z0-9._-]{3,40}")
                || receiver.equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid chat recipient");
        }
        // A browser may only deliver a message to a real, enabled account.
        // Never trust sender, ID, timestamp or readAt supplied by the caller.
        if (users.findByUsername(receiver).filter(u -> u.isEnabled()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return chat.saveMessage(new ChatMessage(principal.getName(), receiver,
                message.getContent().strip()));
    }

    @GetMapping("/thread/{other}")
    public Page<ChatMessage> getThread(@PathVariable String other,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       Principal principal) {
        try {
            return chat.threadFor(principal.getName(), other, page, size);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid chat participant or pagination");
        }
    }

    /**
     * Mark only messages received by the authenticated account from this contact.
     * Repeating the call is harmless and returns zero.
     */
    @PostMapping("/thread/{other}/read")
    public Map<String, Integer> markRead(@PathVariable String other, Principal principal) {
        try {
            return Map.of("marked", chat.markThreadRead(principal.getName(), other));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid chat participant");
        }
    }

    /**
     * Mark a single incoming message read. A foreign or nonexistent ID yields
     * the same zero result; no message content or participant information leaks.
     * Browser calls require the ordinary CSRF token (no exception here).
     */
    @PostMapping("/messages/{id}/read")
    public Map<String, Integer> markMessageRead(@PathVariable Long id, Principal principal) {
        try {
            return Map.of("marked", chat.markMessageRead(principal.getName(), id));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message ID");
        }
    }

    /** Mobile/browser JSON inbox: only contacts of the signed-in account. */
    @GetMapping("/contacts")
    public ResponseEntity<Page<ChatContactSummary>> contacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            Principal principal) {
        try {
            String viewer = principal.getName();
            Page<String> pageData = chat.contactNamesFor(viewer, q, page);
            var unread = chat.unreadCountsForContacts(viewer, pageData.getContent());
            Page<ChatContactSummary> result = pageData.map(contact ->
                    new ChatContactSummary(contact, unread.getOrDefault(contact, 0L)));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(result);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid conversation index filters");
        }
    }

    @GetMapping("/conversation/{sender}/{receiver}")
    public List<ChatMessage> getConversation(@PathVariable String sender,
                                              @PathVariable String receiver, Principal principal) {
        if (!principal.getName().equals(sender) && !principal.getName().equals(receiver))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return chat.getConversation(sender, receiver);
    }

    @GetMapping("/inbox/{receiver}")
    public List<ChatMessage> getInbox(@PathVariable String receiver, Principal principal) {
        if (!principal.getName().equals(receiver))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return chat.getMessagesForUser(receiver);
    }
}
