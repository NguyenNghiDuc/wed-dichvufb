package com.example.fbservice.repository;

import com.example.fbservice.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderAndReceiver(String sender, String receiver);
    List<ChatMessage> findByReceiver(String receiver);
}
