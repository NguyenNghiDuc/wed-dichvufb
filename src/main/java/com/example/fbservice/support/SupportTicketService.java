package com.example.fbservice.support;

import com.example.fbservice.config.TrafficLimiter;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.model.User;
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
    private final SupportAssignmentEventRepository assignments;

    public SupportTicketService(SupportTicketRepository tickets, SupportMessageRepository messages,
                                AccountNotificationRepository notices, TrafficLimiter limiter,
                                UserRepository users, SupportAssignmentEventRepository assignments) {
        this.tickets = tickets;
        this.messages = messages;
        this.notices = notices;
        this.limiter = limiter;
        this.users = users;
        this.assignments = assignments;
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

    /** Constant-memory ticket status summary; each count is scoped in SQL. */
    @Transactional(readOnly = true)
    public SupportTicketCounts countsForOwner(String username) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return new SupportTicketCounts(
            tickets.countByOwnerUsernameAndStatus(username, TicketStatus.OPEN),
            tickets.countByOwnerUsernameAndStatus(username, TicketStatus.ANSWERED),
            tickets.countByOwnerUsernameAndStatus(username, TicketStatus.CLOSED));
    }

    @Transactional(readOnly = true)
    public SupportTicketCounts countsForStaff() {
        return new SupportTicketCounts(
            tickets.countByStatus(TicketStatus.OPEN),
            tickets.countByStatus(TicketStatus.ANSWERED),
            tickets.countByStatus(TicketStatus.CLOSED));
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

    /**
     * Staff queue filter: MINE uses the authenticated staff principal, not a
     * username supplied by the browser. UNASSIGNED excludes already owned work.
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> searchForStaff(int page, String subject, TicketStatus status,
                                               TicketPriority priority, String staff,
                                               TicketAssignmentFilter assignment) {
        if (assignment == null) throw new IllegalArgumentException("Bộ lọc phân công không hợp lệ.");
        Specification<SupportTicket> filters = ticketFilters(null, subject, status, priority);
        if (assignment == TicketAssignmentFilter.MINE) {
            if (staff == null || staff.isBlank())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            filters = filters.and((root, query, cb) -> cb.equal(root.get("assigneeUsername"), staff));
        } else if (assignment == TicketAssignmentFilter.UNASSIGNED) {
            filters = filters.and((root, query, cb) -> cb.isNull(root.get("assigneeUsername")));
        }
        return tickets.findAll(filters, paging(page));
    }

    /** Staff-only owner-username search combined with status and assignment filters. */
    @Transactional(readOnly = true)
    public Page<SupportTicket> searchForStaff(int page, String subject, TicketStatus status,
                                               TicketPriority priority, String staff,
                                               TicketAssignmentFilter assignment, String customer) {
        if (customer == null || customer.isBlank())
            return searchForStaff(page, subject, status, priority, staff, assignment);
        String term = customer.strip().toLowerCase(Locale.ROOT);
        if (term.length() > 80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tài khoản tìm kiếm quá dài.");
        String literal = term.replace("\\", "\\\\")
                             .replace("%", "\\%")
                             .replace("_", "\\_");
        Specification<SupportTicket> filters = ticketFilters(null, subject, status, priority)
                .and((root, query, cb) ->
                    cb.like(cb.lower(root.get("ownerUsername")), "%" + literal + "%", '\\'));
        if (assignment == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (assignment == TicketAssignmentFilter.MINE) {
            if (staff == null || staff.isBlank())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            filters = filters.and((root, query, cb) ->
                cb.equal(root.get("assigneeUsername"), staff));
        } else if (assignment == TicketAssignmentFilter.UNASSIGNED) {
            filters = filters.and((root, query, cb) -> cb.isNull(root.get("assigneeUsername")));
        }
        return tickets.findAll(filters, paging(page));
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

    /** Progress is always loaded through the existing ticket access checks. */
    @Transactional(readOnly = true)
    public SupportTicketProgress progressForOwner(String owner, Long id) {
        return SupportTicketProgress.from(detailForOwner(owner, id),
                java.time.LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public SupportTicketProgress progressForStaff(Long id) {
        return SupportTicketProgress.from(detailForAdmin(id),
                java.time.LocalDateTime.now());
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
        return searchMessagesForOwner(user, id, page, null);
    }

    @Transactional(readOnly = true)
    public Page<SupportMessage> pageMessagesForStaff(Long id, int page) {
        return searchMessagesForStaff(id, page, null);
    }

    @Transactional(readOnly = true)
    public Page<SupportMessage> searchMessagesForOwner(String user, Long id, int page, String term) {
        PageRequest paging = messagePaging(page);
        detailForOwner(user, id);
        return pageFilteredTicketMessages(id, term, paging);
    }

    @Transactional(readOnly = true)
    public Page<SupportMessage> searchMessagesForStaff(Long id, int page, String term) {
        PageRequest paging = messagePaging(page);
        detailForAdmin(id);
        return pageFilteredTicketMessages(id, term, paging);
    }

    private Page<SupportMessage> pageFilteredTicketMessages(Long id, String term, PageRequest paging) {
        String normalized = term == null ? "" : term.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ khóa phản hồi quá dài.");
        return normalized.isEmpty()
            ? messages.findByTicketId(id, paging)
            : messages.searchByBody(id, normalized, paging);
    }

    @Transactional
    public void replyAsOwner(String user, Long id, String message) {
        validate(message, 2, 2000);
        SupportTicket ticket = tickets.lockForOwner(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        limiter.check("support:reply:" + user, 60, Duration.ofHours(1));
        ticket.customerReplied();
        messages.save(new SupportMessage(id, user, message.strip()));
        // Notify only the assigned employee, never copying untrusted message content.
        if (ticket.getAssigneeUsername() != null) {
            notices.save(new AccountNotification(ticket.getAssigneeUsername(),
                "Khách hàng phản hồi hỗ trợ #" + id,
                "Khách hàng đã gửi thêm nội dung. Mở yêu cầu để xem chi tiết."));
        }
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
        ticket.ensureOpen();
        if (staff.equals(ticket.getAssigneeUsername())) return;
        String previous = ticket.getAssigneeUsername();
        ticket.claim(staff);
        assignments.save(new SupportAssignmentEvent(id, previous, staff, staff));
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
                "Hỗ trợ #" + id + " đã được tiếp nhận",
                "Nhân viên hỗ trợ đang xử lý yêu cầu của bạn."));
    }

    /** ADMIN-only recovery from an absent, disabled or unavailable assignee. */
    @Transactional
    public void reassignAsAdmin(String adminUsername, Long id, Long employeeId) {
        User admin = users.findByUsername(adminUsername)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!admin.isEnabled() || !"ADMIN".equals(admin.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.ensureOpen();
        String next = null;
        if (employeeId != null) {
            User employee = users.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tồn tại nhân viên."));
            if (!employee.isEnabled() ||
                !("STAFF".equals(employee.getRole()) || "ADMIN".equals(employee.getRole())))
                throw new IllegalArgumentException("Người nhận phải là STAFF/ADMIN còn hoạt động.");
            next = employee.getUsername();
        }
        String old = ticket.getAssigneeUsername();
        if (java.util.Objects.equals(old, next))
            throw new IllegalArgumentException("Người phụ trách không thay đổi.");
        ticket.reassignTo(next);
        assignments.save(new SupportAssignmentEvent(id, old, next, adminUsername));
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
            "Cập nhật tiếp nhận hỗ trợ #" + id,
            "Yêu cầu của bạn đã được chuyển giao cho bộ phận hỗ trợ."));
        if (next != null)
            notices.save(new AccountNotification(next,
                "Được giao hỗ trợ #" + id, "Bạn đã được phân công xử lý yêu cầu."));
    }

    @Transactional(readOnly = true)
    public boolean isActiveAdmin(String username) {
        return users.findByUsername(username)
            .map(user -> user.isEnabled() && "ADMIN".equals(user.getRole()))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<User> eligibleAssignees() {
        return users.findTop100ByEnabledTrueAndRoleInOrderByUsernameAsc(List.of("STAFF","ADMIN"));
    }

    @Transactional(readOnly = true)
    public List<SupportAssignmentEvent> assignmentHistory(Long id) {
        detailForAdmin(id);
        return assignments.findTop50ByTicketIdOrderByCreatedAtDescIdDesc(id);
    }

    @Transactional
    public void replyAsAdmin(String admin, Long id, String message) {
        validate(message, 2, 2000);
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.ensureAssignedStaff(admin);
        limiter.check("support:staff:reply:" + admin, 180, Duration.ofHours(1));
        ticket.staffReplied();
        messages.save(new SupportMessage(id, admin, message.strip()));
        notices.save(new AccountNotification(ticket.getOwnerUsername(),
            "Hỗ trợ #" + id + " đã phản hồi", "Nhân viên đã phản hồi yêu cầu. Đăng nhập để xem chi tiết."));
    }

    /**
     * Only the exact ticket owner may reopen a CLOSED ticket. A row lock makes
     * two concurrent attempts mutually exclusive; the second request is
     * rejected rather than creating duplicate lifecycle entries.
     * The status change and immutable timeline entry commit atomically.
     */
    @Transactional
    public void reopenAsOwner(String owner, Long id, String reason) {
        validate(reason, 10, 500);
        SupportTicket ticket = tickets.lockForOwner(id, owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ticket.getStatus() != TicketStatus.CLOSED)
            throw new IllegalArgumentException("Chỉ có thể mở lại yêu cầu đã đóng.");
        limiter.check("support:reopen:" + owner, 5, Duration.ofHours(24));
        ticket.reopen();
        // A previously assigned account may have been disabled or demoted
        // while the ticket was closed. Return such a ticket to the unassigned
        // queue and keep an audit event; don't strand it with stale ownership.
        String previousAssignee = ticket.getAssigneeUsername();
        if (previousAssignee != null && users.findByUsername(previousAssignee)
                .filter(u -> u.isEnabled() &&
                        ("STAFF".equals(u.getRole()) || "ADMIN".equals(u.getRole())))
                .isEmpty()) {
            ticket.reassignTo(null);
            assignments.save(new SupportAssignmentEvent(
                    id, previousAssignee, null, "HỆ THỐNG"));
        }
        messages.save(new SupportMessage(id, "HỆ THỐNG",
            "Trạng thái thay đổi: ĐÃ ĐÓNG → ĐANG CHỜ (khách hàng yêu cầu mở lại)."));
        // Reason belongs to this ticket's private, paginated timeline only;
        // Thymeleaf renders it with th:text (escaped), never as HTML.
        messages.save(new SupportMessage(id, owner, reason.strip()));
    }

    @Transactional
    public void closeAsAdmin(String admin, Long id) {
        closeAsAdmin(admin, id, null);
    }

    /** Optional closing note is private to this ticket and HTML-escaped by the view. */
    @Transactional
    public void closeAsAdmin(String admin, Long id, String resolutionNote) {
        if (resolutionNote != null && !resolutionNote.isBlank())
            validate(resolutionNote, 10, 500);
        SupportTicket ticket = tickets.lockForAdmin(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ticket.ensureAssignedStaff(admin);
        // Preserve the actual prior state: an unassigned OPEN ticket may be
        // closed without ever reaching ANSWERED.
        String previousStatus = ticket.getStatus().getTitle();
        ticket.close();
        messages.save(new SupportMessage(id, "HỆ THỐNG",
            "Trạng thái thay đổi: " + previousStatus + " → Đã đóng (bởi nhân viên hỗ trợ)."));
        if (resolutionNote != null && !resolutionNote.isBlank())
            messages.save(new SupportMessage(id, admin, resolutionNote.strip()));
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
