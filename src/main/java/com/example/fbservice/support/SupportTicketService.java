package com.example.fbservice.support;

import com.example.fbservice.config.TrafficLimiter;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.notification.*;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
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
    private final UserRepository users;

    public SupportTicketService(SupportTicketRepository tickets, SupportMessageRepository messages,
                                AccountNotificationRepository notices, TrafficLimiter limiter,
                                UserRepository users) {
        this.tickets = tickets;
        this.messages = messages;
        this.notices = notices;
        this.limiter = limiter;
        this.users = users;
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

    /**
     * Bounded subject search. A caller cannot inject an arbitrary owner:
     * the predicate uses the authenticated principal as its exact owner.
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> searchMine(String user, int page, String subject, TicketStatus status) {
        if (user == null || user.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return tickets.findAll(ticketFilters(user, subject, status, null), paging(page));
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> searchForStaff(int page, String subject, TicketStatus status,
                                               TicketPriority priority) {
        return tickets.findAll(ticketFilters(null, subject, status, priority), paging(page));
    }

    private Specification<SupportTicket> ticketFilters(String owner, String subject,
                                                         TicketStatus status, TicketPriority priority) {
        String cleaned = subject == null ? "" : subject.strip();
        if (cleaned.length() > 80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ khóa quá dài.");
        // Treat LIKE metacharacters as plain characters; no regex or user-supplied SQL.
        String literal = cleaned.toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return (root, query, cb) -> {
            List<Predicate> checks = new ArrayList<>();
            if (owner != null) checks.add(cb.equal(root.get("ownerUsername"), owner));
            if (!cleaned.isBlank())
                checks.add(cb.like(cb.lower(root.get("subject")), "%" + literal + "%", '\\'));
            if (status != null) checks.add(cb.equal(root.get("status"), status));
            if (priority != null) checks.add(cb.equal(root.get("priority"), priority));
            return cb.and(checks.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public void prioritizeAsStaff(Long id, TicketPriority priority) {
        if (priority == null) throw new IllegalArgumentException("Mức ưu tiên không hợp lệ.");
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ticket.getStatus() == TicketStatus.CLOSED)
            throw new IllegalArgumentException("Không đổi ưu tiên yêu cầu đã đóng.");
        ticket.setPriority(priority);
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

    /**
     * A ticket can be claimed once. A repeated claim by the same staff member
     * is idempotent; another staff member receives a conflict, under row locking.
     */
    @Transactional
    public void claimAsStaff(String staff, Long id) {
        var user = users.findByUsername(staff)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!user.isEnabled() || !("STAFF".equals(user.getRole()) || "ADMIN".equals(user.getRole())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (staff.equals(ticket.getAssigneeUsername())) return;
        ticket.claim(staff);
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
                "Hỗ trợ #" + id + " đã được tiếp nhận",
                "Nhân viên hỗ trợ đang xử lý yêu cầu của bạn."));
    }

    @Transactional
    public void replyAsAdmin(String admin, Long id, String message) {
        validate(message, 2, 2000);
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.ensureAssignedStaff(admin);
        ticket.staffReplied();
        messages.save(new SupportMessage(id, admin, message.strip()));
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
            "Hỗ trợ #" + id + " đã phản hồi", "Nhân viên đã phản hồi yêu cầu. Đăng nhập để xem chi tiết."));
    }

    @Transactional
    public void closeAsAdmin(String admin, Long id) {
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.ensureAssignedStaff(admin);
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
