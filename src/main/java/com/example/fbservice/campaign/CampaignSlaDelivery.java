package com.example.fbservice.campaign;

import com.example.fbservice.notification.*;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampaignSlaDelivery {
    private final CampaignOrderRepository orders;
    private final AccountNotificationRepository notifications;

    public CampaignSlaDelivery(CampaignOrderRepository orders, AccountNotificationRepository notifications) {
        this.orders=orders;
        this.notifications=notifications;
    }

    /** Lock the order so multiple scheduler instances cannot duplicate alerts. */
    @Transactional
    public boolean deliverIfOverdue(Long id, LocalDateTime utcNow) {
        CampaignOrder order=orders.lockById(id)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!order.isOverdueAt(utcNow) || order.getOverdueNotifiedAt()!=null) return false;
        order.markOverdueAlertSent(utcNow);
        notifications.save(new AccountNotification(order.getOwnerUsername(),
            "Chiến dịch #" + id + " cần kiểm tra tiến độ",
            "Đơn đã vượt mốc xử lý nội bộ; nhân viên sẽ kiểm tra."));
        if(order.getAssigneeUsername()!=null) {
            notifications.save(new AccountNotification(order.getAssigneeUsername(),
                "Chiến dịch #" + id + " quá hạn nội bộ",
                "Vui lòng kiểm tra và cập nhật tiến độ."));
        }
        return true;
    }
}
