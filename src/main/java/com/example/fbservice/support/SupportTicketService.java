package com.example.fbservice.support;

import com.example.fbservice.config.TrafficLimiter;
import com.example.fbservice.notification.*;
import java.time.Duration;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportTicketService {
    private final SupportTicketRepository tickets;
    private final SupportMessageRepository messages;
    private final AccountNotificationRepository notices;
    private final TrafficLimiter limiter;

    public SupportTicketService(SupportTicketRepository tickets, SupportMessageRepository messages,
                                AccountNotificationRepository notices, TrafficLimiter limiter) {
        this.tickets = tickets;
        this.messages = messages;
        this.notices = notices;
        this.limiter = limiter;
    }

    @Transactional
    public SupportTicket create(String user, String subject, String description) {
        validate(subject, 4, 120);
        validate(description, 10, 2000);
        limiter.check("support:create:" + user, 5, Duration.ofHours(1));
        SupportTicket ticket = tickets.save(new SupportTicket(user, subject.strip(), description.strip()));
        notices.save(new AccountNotification(user, "Đã nhận hỗ trợ #" + ticket.getId(),
                "Chúng tôi đã tiếp nhận yêu cầu hỗ trợ của bạn."));
        return ticket;
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> listMine(String user, int page) {
        return tickets.findByOwnerUsername(user, paging(page));
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> listForAdmin(int page) {
        return tickets.findAll(paging(page));
    }

    @Transactional(readOnly = true)
    public SupportTicket detailForOwner(String user, Long id) {
        return tickets.findByIdAndOwnerUsername(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public SupportTicket detailForAdmin(Long id) {
        return tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<SupportMessage> messagesForOwner(String user, Long id) {
        detailForOwner(user, id);
        return messages.findByTicketIdOrderByCreatedAtAsc(id);
    }

    @Transactional(readOnly = true)
    public List<SupportMessage> messagesForAdmin(Long id) {
        detailForAdmin(id);
        return messages.findByTicketIdOrderByCreatedAtAsc(id);
    }

    private PageRequest messagePaging(int page) {
        if (page < 0 || page > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang tin nhắn không hợp lệ.");
        return PageRequest.of(page, 20,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
    }

    @Transactional(readOnly = true)
    public Page<SupportMessage> pageMessagesForOwner(String user, Long id, int page) {
        PageRequest paging = messagePaging(page);
        detailForOwner(user, id);
        return messages.findByTicketId(id, paging);
    }

    @Transactional(readOnly = true)
    public Page<SupportMessage> pageMessagesForStaff(Long id, int page) {
        PageRequest paging = messagePaging(page);
        detailForAdmin(id);
        return messages.findByTicketId(id, paging);
    }

    @Transactional
    public void replyAsOwner(String user, Long id, String message) {
        validate(message, 2, 2000);
        SupportTicket ticket = tickets.lockForOwner(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.customerReplied();
        messages.save(new SupportMessage(id, user, message.strip()));
    }

    @Transactional
    public void replyAsAdmin(String admin, Long id, String message) {
        validate(message, 2, 2000);
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.staffReplied();
        messages.save(new SupportMessage(id, admin, message.strip()));
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
            "Hỗ trợ #" + id + " đã phản hồi", "Nhân viên đã phản hồi yêu cầu. Đăng nhập để xem chi tiết."));
    }

    @Transactional
    public void closeAsAdmin(String admin, Long id) {
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.close();
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
            "Hỗ trợ #" + id + " đã đóng", "Yêu cầu đã được đóng bởi nhân viên " + admin + "."));
    }

    private Pageable paging(int page) {
        if (page < 0 || page > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang không hợp lệ.");
        return PageRequest.of(page, 20, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
    }

    private void validate(String value, int min, int max) {
        if (value == null || value.strip().length() < min || value.length() > max)
            throw new IllegalArgumentException("Nội dung cần từ " + min + " đến " + max + " ký tự.");
    }
}
