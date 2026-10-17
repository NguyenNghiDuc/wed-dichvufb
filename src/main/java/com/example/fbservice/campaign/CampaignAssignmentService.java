package com.example.fbservice.campaign;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.notification.*;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampaignAssignmentService {
    private final CampaignOrderRepository orders;
    private final UserRepository users;
    private final CampaignAssignmentEventRepository assignmentEvents;
    private final CampaignEventRepository campaignEvents;
    private final AccountNotificationRepository notifications;

    public CampaignAssignmentService(CampaignOrderRepository orders, UserRepository users,
            CampaignAssignmentEventRepository assignmentEvents,
            CampaignEventRepository campaignEvents, AccountNotificationRepository notifications) {
        this.orders = orders;
        this.users = users;
        this.assignmentEvents = assignmentEvents;
        this.campaignEvents = campaignEvents;
        this.notifications = notifications;
    }

    @Transactional
    public CampaignOrder assign(Long campaignId, Long employeeId, String adminUsername) {
        User admin = users.findByUsername(adminUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!admin.isEnabled() || !"ADMIN".equals(admin.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        CampaignOrder order = orders.lockById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (order.getStatus() == CampaignStatus.COMPLETED ||
                order.getStatus() == CampaignStatus.CANCELLED ||
                order.getStatus() == CampaignStatus.REJECTED) {
            throw new IllegalArgumentException("Không thể phân công đơn đã đóng.");
        }
        String next = null;
        if (employeeId != null) {
            User employee = users.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tồn tại nhân viên."));
            if (!employee.isEnabled() ||
                    !(employee.getRole().equals("STAFF") || employee.getRole().equals("ADMIN"))) {
                throw new IllegalArgumentException("Chỉ phân công cho tài khoản STAFF/ADMIN đang hoạt động.");
            }
            next = employee.getUsername();
        }
        String previous = order.getAssigneeUsername();
        if (java.util.Objects.equals(previous, next))
            throw new IllegalArgumentException("Nhân viên phụ trách không thay đổi.");

        order.assignTo(next);
        assignmentEvents.save(new CampaignAssignmentEvent(order.getId(), previous, next, adminUsername));
        String label = next == null ? "Chưa phân công" : next;
        notifications.save(new AccountNotification(order.getOwnerUsername(),
                "Phân công chiến dịch #" + campaignId,
                "Người phụ trách mới: " + label));
        if (next != null) {
            notifications.save(new AccountNotification(next,
                "Bạn được giao chiến dịch #" + campaignId,
                "Kiểm tra danh sách công việc được phân công."));
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<User> eligibleStaff() {
        return users.findTop100ByEnabledTrueAndRoleInOrderByUsernameAsc(
                List.of("STAFF", "ADMIN"));
    }

    @Transactional(readOnly = true)
    public Page<CampaignOrder> assignedTo(String staffUsername, int page) {
        ensureStaff(staffUsername);
        if (page < 0 || page > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return orders.findByAssigneeUsername(staffUsername,
                PageRequest.of(page, 20, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
    }

    @Transactional
    public CampaignOrder progress(Long campaignId, CampaignStatus next, String staffUsername) {
        ensureStaff(staffUsername);
        CampaignOrder order = orders.lockAssigned(campaignId, staffUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!((order.getStatus() == CampaignStatus.APPROVED && next == CampaignStatus.IN_PROGRESS)
                || (order.getStatus() == CampaignStatus.IN_PROGRESS && next == CampaignStatus.COMPLETED))) {
            throw new IllegalArgumentException("Nhân viên chỉ có thể bắt đầu hoặc hoàn thành đơn được duyệt.");
        }
        CampaignStatus from = order.getStatus();
        order.changeStatus(next);
        campaignEvents.save(new CampaignEvent(order.getId(), from, next, staffUsername,
                "Nhân viên phụ trách cập nhật tiến độ"));
        notifications.save(new AccountNotification(order.getOwnerUsername(),
                "Tiến độ chiến dịch #" + campaignId,
                "Trạng thái mới: " + next.getDisplayName()));
        return order;
    }

    @Transactional(readOnly = true)
    public List<CampaignAssignmentEvent> assignmentHistory(String adminUsername, Long campaignId) {
        User admin = users.findByUsername(adminUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!admin.isEnabled() || !"ADMIN".equals(admin.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!orders.existsById(campaignId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return assignmentEvents.findByCampaignIdOrderByCreatedAtAsc(campaignId);
    }

    private void ensureStaff(String username) {
        User staff = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!staff.isEnabled() || !(staff.getRole().equals("STAFF") || staff.getRole().equals("ADMIN")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
