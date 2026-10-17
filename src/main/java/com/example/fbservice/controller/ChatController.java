package com.example.fbservice.controller;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
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

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @PostMapping("/send")
    public ChatMessage sendMessage(@RequestBody ChatMessage message, Principal principal) {
        if (message == null || message.getReceiver() == null || message.getReceiver().isBlank()
            || message.getReceiver().length() > 100 || message.getContent() == null ||
            message.getContent().isBlank() || message.getContent().length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message");
        // Ignore the request body's sender and ID. Only the authenticated user can author a message.
        ChatMessage safe = new ChatMessage(principal.getName(),
                message.getReceiver().trim(), message.getContent());
        return chat.saveMessage(safe);
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
