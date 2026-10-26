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

    private final com.example.fbservice.repository.UserRepository users;
    private final com.example.fbservice.config.TrafficLimiter limiter;
    public ChatService(ChatMessageRepository messages,com.example.fbservice.repository.UserRepository users,
                       com.example.fbservice.config.TrafficLimiter limiter) {
        this.messages=messages;this.users=users;this.limiter=limiter;
    }

    @Transactional
    public ChatMessage sendAuthorized(String sender,String receiver,String content) {
        if(sender==null||receiver==null||!receiver.matches("[a-zA-Z0-9._-]{3,40}")||content==null||content.isBlank()||content.length()>255)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Invalid message");
        var from=users.findByUsername(sender).filter(com.example.fbservice.model.User::isEnabled)
            .orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        var to=users.findByUsername(receiver).filter(com.example.fbservice.model.User::isEnabled)
            .orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        var staff=java.util.Set.of("ADMIN","STAFF");
        if(sender.equals(receiver)||!staff.contains(from.getRole())&&!staff.contains(to.getRole()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Chat is available with support staff only");
        limiter.check("chat:send:"+sender,60,java.time.Duration.ofMinutes(1));
        return messages.save(new ChatMessage(sender,receiver,content.strip()));
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
