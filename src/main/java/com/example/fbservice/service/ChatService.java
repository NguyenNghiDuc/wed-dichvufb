package com.example.fbservice.service;

import com.example.fbservice.model.ChatMessage;
import com.example.fbservice.config.TrafficLimiter;
import java.time.Duration;
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
    private final TrafficLimiter limiter;

    public ChatService(ChatMessageRepository messages, UserRepository users,
                       AccountNotificationRepository notifications, TrafficLimiter limiter) {
        this.messages = messages;
        this.users = users;
        this.notifications = notifications;
        this.limiter = limiter;
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        if (message == null || message.getSender() == null || message.getSender().isBlank())
            throw new IllegalArgumentException("Sender is required");
        limiter.check("chat:send:" + message.getSender(), 120, Duration.ofHours(1));
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

    /**
     * Private, per-message read receipt; repeat and unrelated-account attempts
     * both return zero without disclosing existence of a message.
     */
    @Transactional
    public int markMessageRead(String viewer, Long messageId) {
        if (viewer == null || viewer.isBlank())
            throw new IllegalArgumentException("Authentication required");
        if (messageId == null || messageId <= 0)
            throw new IllegalArgumentException("Invalid message ID");
        return messages.markOneRead(viewer, messageId, LocalDateTime.now());
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

    /** Private, bounded conversation index without scanning or loading all messages in Java. */
    @Transactional(readOnly = true)
    public Page<String> contactNamesFor(String viewer, String term, int page) {
        if (viewer == null || viewer.isBlank())
            throw new IllegalArgumentException("Authentication required");
        if (page < 0 || page > MAX_PAGE)
            throw new IllegalArgumentException("Invalid contact page");
        String query = term == null ? "" : term.strip().toLowerCase(java.util.Locale.ROOT);
        if (query.length() > 40 || !query.matches("[a-z0-9._-]*"))
            throw new IllegalArgumentException("Invalid contact search");
        return messages.findContactNames(viewer, query, PageRequest.of(page, 20));
    }

    /**
     * Query only the 20 contacts that the signed-in user's inbox already returned.
     * Missing groups imply zero unread messages; no third-party counts are exposed.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> unreadCountsForContacts(String viewer, List<String> contacts) {
        if (viewer == null || viewer.isBlank() || contacts == null || contacts.size() > 20)
            throw new IllegalArgumentException("Invalid scoped contact page");
        if (contacts.isEmpty()) return java.util.Map.of();
        java.util.Map<String, Long> totals = new java.util.HashMap<>();
        for (String contact : contacts) totals.put(contact, 0L);
        for (ChatMessageRepository.UnreadContactCount row :
                messages.countUnreadByContact(viewer, contacts)) {
            totals.put(row.getContact(), row.getUnreadCount());
        }
        return totals;
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
