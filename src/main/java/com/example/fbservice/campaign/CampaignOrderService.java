package com.example.fbservice.campaign;

import com.example.fbservice.notification.*;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampaignOrderService {
    private final CampaignOrderRepository orders;
    private final CampaignEventRepository events;
    private final AccountNotificationRepository notifications;

    public CampaignOrderService(CampaignOrderRepository orders, CampaignEventRepository events,
                                AccountNotificationRepository notifications) {
        this.orders = orders;
        this.events = events;
        this.notifications = notifications;
    }

    @Transactional
    public CampaignOrder create(String username, CampaignOrderRequest request) {
        if (username == null || username.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        validate(request);
        CampaignOrder order = orders.save(new CampaignOrder(username, request));
        events.save(new CampaignEvent(order.getId(), null, CampaignStatus.PENDING_REVIEW, username, "Chiến dịch được tạo"));
        notifications.save(new AccountNotification(username, "Đã nhận chiến dịch #" + order.getId(),
                "Yêu cầu đang chờ kiểm duyệt."));
        return order;
    }

    @Transactional(readOnly = true)
    public List<CampaignOrder> findMine(String username) {
        return orders.findByOwnerUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public Page<CampaignOrder> pageMine(String username, int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 50)
            throw new IllegalArgumentException("Giới hạn phân trang: page 0–10000, size 1–50.");
        return orders.findByOwnerUsername(username, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @Transactional(readOnly = true)
    public Page<CampaignOrder> pageForAdmin(int page) {
        if (page < 0 || page > 10000)
            throw new IllegalArgumentException("Trang không hợp lệ.");
        return orders.findAll(PageRequest.of(page, 20,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @Transactional(readOnly = true)
    public CampaignOrder findMineById(String username, Long id) {
        return orders.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
