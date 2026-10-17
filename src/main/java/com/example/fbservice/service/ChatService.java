package com.example.fbservice.service;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.repository.ChatMessageRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private static final int MAX_PAGE = 10_000;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("timestamp"), Sort.Order.desc("id"));

    private final ChatMessageRepository messages;

    public ChatService(ChatMessageRepository messages) {
        this.messages = messages;
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        return messages.save(message);
    }

    /** Both directions, always scoped to the logged-in user by the controller. */
    @Transactional(readOnly = true)
    public Page<ChatMessage> threadFor(String viewer, String other, int page, int size) {
        if (viewer == null || viewer.isBlank())
            throw new IllegalArgumentException("Authentication required");
        if (other == null || !other.matches("[a-zA-Z0-9._-]{3,40}"))
            throw new IllegalArgumentException("Invalid conversation participant");
        return messages.findThread(viewer, other, pageRequest(page, size));
    }

    /** Compatibility: legacy endpoint returns only the latest 50 messages in one direction. */
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversation(String sender, String receiver) {
        return messages.findBySenderAndReceiver(sender, receiver,
                pageRequest(0, MAX_PAGE_SIZE)).getContent();
    }

    /** Compatibility: legacy inbox returns the latest 50 received messages, not all history. */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForUser(String receiver) {
        return messages.findByReceiver(receiver, pageRequest(0, MAX_PAGE_SIZE)).getContent();
    }

    private static PageRequest pageRequest(int page, int size) {
        if (page < 0 || page > MAX_PAGE || size < 1 || size > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("page must be 0-10000 and size must be 1-50");
        return PageRequest.of(page, size, NEWEST_FIRST);
    }
}
