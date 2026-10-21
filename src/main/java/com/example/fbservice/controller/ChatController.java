package com.example.fbservice.controller;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.service.ChatService;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
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

    public record SendMessage(String receiver,String content) {}
    public record MessageView(Long id,String sender,String receiver,String content,java.time.LocalDateTime timestamp) {
        static MessageView from(ChatMessage m){return new MessageView(m.getId(),m.getSender(),m.getReceiver(),m.getContent(),m.getTimestamp());}
    }
    @PostMapping("/send")
    public MessageView sendMessage(@RequestBody SendMessage message, Principal principal) {
        return MessageView.from(chat.sendAuthorized(principal.getName(),message.receiver(),message.content()));
    }

    @GetMapping("/thread/{other}")
    public Page<MessageView> getThread(@PathVariable String other,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       Principal principal) {
        try {
            return chat.threadFor(principal.getName(), other, page, size).map(MessageView::from);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid chat participant or pagination");
        }
    }

    @GetMapping("/conversation/{sender}/{receiver}")
    public List<MessageView> getConversation(@PathVariable String sender,
                                              @PathVariable String receiver, Principal principal) {
        if (!principal.getName().equals(sender) && !principal.getName().equals(receiver))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return chat.getConversation(sender, receiver).stream().map(MessageView::from).toList();
    }

    @GetMapping("/inbox/{receiver}")
    public List<MessageView> getInbox(@PathVariable String receiver, Principal principal) {
        if (!principal.getName().equals(receiver))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return chat.getMessagesForUser(receiver).stream().map(MessageView::from).toList();
    }
}
