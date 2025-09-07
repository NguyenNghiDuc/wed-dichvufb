package com.example.fbservice.controller;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Gửi tin nhắn
    @PostMapping("/send")
    public ResponseEntity<ChatMessage> sendMessage(@RequestBody ChatMessage message) {
        return ResponseEntity.ok(chatService.saveMessage(message));
    }

    // Lấy tin nhắn giữa 2 người
    @GetMapping("/conversation/{sender}/{receiver}")
    public ResponseEntity<List<ChatMessage>> getConversation(
            @PathVariable String sender,
            @PathVariable String receiver) {
        return ResponseEntity.ok(chatService.getConversation(sender, receiver));
    }

    // Lấy tất cả tin nhắn gửi đến user
    @GetMapping("/inbox/{receiver}")
    public ResponseEntity<List<ChatMessage>> getInbox(@PathVariable String receiver) {
        return ResponseEntity.ok(chatService.getMessagesForUser(receiver));
    }
}
