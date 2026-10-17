package com.example.fbservice.campaign;

import com.example.fbservice.notification.*;
import jakarta.persistence.criteria.Predicate;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampaignOrderService {
    private final com.example.fbservice.catalog.ServiceCatalogService catalog;
    private final CampaignOrderRepository orders;
    private final CampaignEventRepository events;
    private final AccountNotificationRepository notifications;

    public CampaignOrderService(CampaignOrderRepository orders, CampaignEventRepository events,
                                AccountNotificationRepository notifications,
                                com.example.fbservice.catalog.ServiceCatalogService catalog) {
        this.catalog=catalog;
        this.orders = orders;
        this.events = events;
        this.notifications = notifications;
    }

    @Transactional
    public CampaignOrder create(String username, CampaignOrderRequest request) {
        if (username == null || username.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        validate(request);
        CampaignOrder order = orders.save(new CampaignOrder(username, request, catalog.quote(request.serviceType())));
        events.save(new CampaignEvent(order.getId(), null, CampaignStatus.PENDING_REVIEW, username, "Chiến dịch được tạo"));
        notifications.save(new AccountNotification(username, "Đã nhận chiến dịch #" + order.getId(),
                "Yêu cầu đang chờ kiểm duyệt."));
        return order;
    }

    /** Provisional quote from the live enabled service catalog; never accept a client-provided amount. */
    @Transactional(readOnly = true)
    public CampaignQuote quoteForCustomer(String username, CampaignServiceType serviceType) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (serviceType == null) throw new IllegalArgumentException("Dịch vụ không hợp lệ.");
        return new CampaignQuote(serviceType, serviceType.getDisplayName(),
            catalog.quote(serviceType), "VND", true);
    }

    @Transactional(readOnly = true)
    public CampaignSummary summaryForOwner(String username) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        long[] counts = new long[CampaignStatus.values().length];
        for (CampaignOrderRepository.OwnerStatusCount count : orders.countStatusesForOwner(username)) {
            counts[count.getStatus().ordinal()] = count.getTotal();
        }
        long total = 0;
        for (long count : counts) total += count;
        return new CampaignSummary(total,
            counts[CampaignStatus.PENDING_REVIEW.ordinal()],
            counts[CampaignStatus.APPROVED.ordinal()],
            counts[CampaignStatus.IN_PROGRESS.ordinal()],
            counts[CampaignStatus.COMPLETED.ordinal()],
            counts[CampaignStatus.REJECTED.ordinal()],
            counts[CampaignStatus.CANCELLED.ordinal()]);
    }

    @Transactional(readOnly = true)
    public List<CampaignOrder> findMine(String username) {
        return orders.findByOwnerUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public Page<CampaignOrder> pageMine(String username, int page, int size) {
        return pageMine(username, page, size, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<CampaignOrder> pageMine(String username, int page, int size,
                                        Long orderId, CampaignStatus status) {
        return pageMine(username, page, size, orderId, status, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<CampaignOrder> pageMine(String username, int page, int size,
                                        Long orderId, CampaignStatus status,
                                        CampaignServiceType serviceType, LocalDate from, LocalDate to) {
        if (username == null || username.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (page < 0 || page > 10000 || size < 1 || size > 50)
            throw new IllegalArgumentException("Giới hạn phân trang: page 0–10000, size 1–50.");
        if (orderId != null && orderId <= 0)
            throw new IllegalArgumentException("Mã đơn phải là số nguyên dương.");
        validateDateRange(from, to);
        LocalDateTime lowerBound = from == null ? null : from.atStartOfDay();
        LocalDateTime upperBound = to == null ? null : to.plusDays(1).atStartOfDay();

        // Ownership is mandatory inside the database query (including the count query).
        // Criteria predicates avoid nullable enum/ID SQL parameters on PostgreSQL.
        Specification<CampaignOrder> filters = (root, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(builder.equal(root.get("ownerUsername"), username));
            if (orderId != null) conditions.add(builder.equal(root.get("id"), orderId));
            if (status != null) conditions.add(builder.equal(root.get("status"), status));
            if (serviceType != null) conditions.add(builder.equal(root.get("serviceType"), serviceType));
            if (lowerBound != null) conditions.add(builder.greaterThanOrEqualTo(root.get("createdAt"), lowerBound));
            if (upperBound != null) conditions.add(builder.lessThan(root.get("createdAt"), upperBound));
            return builder.and(conditions.toArray(new Predicate[0]));
        };
        return orders.findAll(filters, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    /** Inclusive dates in the application's timezone with an exclusive next-day upper bound. */
    private static void validateDateRange(LocalDate from, LocalDate to) {
        if ((from != null && (from.getYear() < 2000 || from.getYear() > 2100)) ||
            (to != null && (to.getYear() < 2000 || to.getYear() > 2100)) ||
            (from != null && to != null && from.isAfter(to)))
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ (2000–2100).");
    }

    @Transactional(readOnly = true)
    public Page<CampaignOrder> pageForAdmin(int page) {
        return pageForAdmin(page, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<CampaignOrder> pageForAdmin(int page, Long orderId, CampaignStatus status, String owner) {
        if (page < 0 || page > 10000)
            throw new IllegalArgumentException("Trang không hợp lệ.");
        if (orderId != null && orderId <= 0)
            throw new IllegalArgumentException("Mã đơn phải là số nguyên dương.");
        String exactOwner = owner == null || owner.isBlank() ? null : owner.trim();
        if (exactOwner != null && (exactOwner.length() > 100 ||
                !exactOwner.matches("[a-zA-Z0-9._-]{3,100}")))
            throw new IllegalArgumentException("Tên khách hàng không hợp lệ.");

        // Admin-only query: no wildcard owner match; bounded and deterministic results.
        // Separate from the customer-scoped method to avoid accidental privilege expansion.
        Specification<CampaignOrder> filters = (root, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (orderId != null) conditions.add(builder.equal(root.get("id"), orderId));
            if (status != null) conditions.add(builder.equal(root.get("status"), status));
            if (exactOwner != null) conditions.add(builder.equal(root.get("ownerUsername"), exactOwner));
            return builder.and(conditions.toArray(new Predicate[0]));
        };
        return orders.findAll(filters, PageRequest.of(page, 20,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @Transactional(readOnly = true)
    public CampaignOrder findMineById(String username, Long id) {
        return orders.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<CampaignHistoryEntry> pageHistoryForOwner(String username, Long id, int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 50)
            throw new IllegalArgumentException("Giới hạn lịch sử: page 0–10000, size 1–50.");
        // Verify ownership before accessing the event table. Other users receive 404.
        findMineById(username, id);
        PageRequest paging = PageRequest.of(page, size,
            Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        return events.findByCampaignId(id, paging).map(e ->
            new CampaignHistoryEntry(e.getOldStatus(), e.getNewStatus(),
                e.getDescription(), e.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public List<CampaignEvent> historyForOwner(String username, Long id) {
        findMineById(username, id);
        return events.findByCampaignIdOrderByCreatedAtAsc(id);
    }

    @Transactional(readOnly = true)
    public List<CampaignOrder> findAllForAdmin() {
        return orders.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public CampaignOrder cancelForOwner(String username, Long id) {
        CampaignOrder order = orders.lockByIdAndOwner(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (order.getStatus() != CampaignStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Chỉ có thể hủy chiến dịch chưa được duyệt.");
        }
        CampaignStatus previous = order.getStatus();
        order.changeStatus(CampaignStatus.CANCELLED);
        events.save(new CampaignEvent(id, previous, order.getStatus(), username, "Khách hàng hủy yêu cầu"));
        notifications.save(new AccountNotification(username, "Đã hủy chiến dịch #" + id,
                "Đơn đã hủy trước khi được duyệt; không phát sinh thanh toán."));
        return order;
    }

    @Transactional
    public CampaignOrder updateStatusForAdmin(Long id, CampaignStatus status, String actor) {
        CampaignOrder order = orders.lockById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        CampaignStatus from = order.getStatus();
        order.changeStatus(status);
        events.save(new CampaignEvent(id, from, status, actor, "Quản trị viên cập nhật trạng thái"));
        notifications.save(new AccountNotification(order.getOwnerUsername(), "Chiến dịch #" + id + " đã cập nhật",
                "Trạng thái mới: " + status.getDisplayName()));
        return order;
    }

    private void validate(CampaignOrderRequest request) {
        if (request == null || request.platform() == null || request.serviceType() == null ||
                request.targetUrl() == null || request.targetUrl().length() > 512 ||
                request.brief() == null || request.brief().trim().length() < 10 ||
                request.brief().length() > 1000)
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin chiến dịch.");
        try {
            URI uri = URI.create(request.targetUrl().trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null ||
                    uri.getPort() != -1 || uri.getHost() == null || !request.platform().acceptsHost(uri.getHost()))
                throw new IllegalArgumentException("Link HTTPS phải thuộc nền tảng đã chọn.");
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Link HTTPS phải thuộc nền tảng đã chọn.");
        }
    }
}
