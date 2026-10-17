package com.example.fbservice.service;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.repository.ChatMessageRepository;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.notification.AccountNotification;
import com.example.fbservice.notification.AccountNotificationRepository;
import java.time.LocalDateTime;
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
    private final UserRepository users;
    private final AccountNotificationRepository notifications;

    public ChatService(ChatMessageRepository messages, UserRepository users,
                       AccountNotificationRepository notifications) {
        this.messages = messages;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        ChatMessage saved = messages.save(message);
        // Never send user-controlled message bodies in notifications.
        // No notification rows for nonexistent or self-addressed accounts.
        if (!saved.getSender().equals(saved.getReceiver()) &&
                users.existsByUsername(saved.getReceiver())) {
            notifications.save(new AccountNotification(saved.getReceiver(),
                "Tin nhắn mới", "Có phản hồi mới từ " + saved.getSender() + "."));
        }
        return saved;
    }

    @Transactional
    public int markThreadRead(String viewer, String other) {
        if (viewer == null || viewer.isBlank() || other == null ||
                !other.matches("[a-zA-Z0-9._-]{3,40}"))
            throw new IllegalArgumentException("Invalid conversation participant");
        return messages.markThreadRead(viewer, other, LocalDateTime.now());
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
