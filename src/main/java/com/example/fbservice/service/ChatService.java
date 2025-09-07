package com.example.fbservice.service;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getConversation(String sender, String receiver) {
        return chatMessageRepository.findBySenderAndReceiver(sender, receiver);
    }

    public List<ChatMessage> getMessagesForUser(String receiver) {
        return chatMessageRepository.findByReceiver(receiver);
    }
}
