package com.example.fbservice.controller;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
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
        if (message.getReceiver() == null || message.getReceiver().isBlank()
            || message.getContent() == null || message.getContent().isBlank()
            || message.getContent().length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message");
        }
        ChatMessage safe = new ChatMessage(principal.getName(), message.getReceiver().trim(), message.getContent());
        return chat.saveMessage(safe);
    }

    @GetMapping("/conversation/{sender}/{receiver}")
    public List<ChatMessage> getConversation(@PathVariable String sender, @PathVariable String receiver,
                                               Principal principal) {
        if (!principal.getName().equals(sender) && !principal.getName().equals(receiver)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return chat.getConversation(sender, receiver);
    }

    @GetMapping("/inbox/{receiver}")
    public List<ChatMessage> getInbox(@PathVariable String receiver, Principal principal) {
        if (!principal.getName().equals(receiver)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return chat.getMessagesForUser(receiver);
    }
}
