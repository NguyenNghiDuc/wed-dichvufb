package com.example.fbservice.campaign;

import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** Provisional operational thresholds aligned with existing admin counters. */
@Service
public class CampaignAttentionService {
    static final long REVIEW_HOURS = 48;
    static final long EXECUTION_HOURS = 168;

    public CampaignAttentionView inspect(CampaignOrder order) {
        return inspect(order, LocalDateTime.now());
    }

    // Pure function to permit deterministic, boundary-specific unit tests.
    public CampaignAttentionView inspect(CampaignOrder order, LocalDateTime now) {
        if (order.getStatus() == CampaignStatus.REJECTED) {
            return new CampaignAttentionView(true, false, "Đã từ chối",
                "Đơn đã bị từ chối; nếu cần giải thích, hãy gửi yêu cầu hỗ trợ. Chưa có hoàn tiền tự động.");
        }
        if (order.getStatus() == CampaignStatus.PENDING_REVIEW &&
            Duration.between(order.getCreatedAt(), now).toHours() >= REVIEW_HOURS) {
            return new CampaignAttentionView(true, true, "Chờ duyệt lâu",
                "Đơn chờ duyệt vượt mốc nhắc việc nội bộ 48 giờ; vui lòng liên hệ hỗ trợ nếu cần.");
        }
        if (order.getStatus() == CampaignStatus.IN_PROGRESS &&
            Duration.between(order.getUpdatedAt(), now).toHours() >= EXECUTION_HOURS) {
            return new CampaignAttentionView(true, true, "Đang xử lý lâu",
                "Đơn đang xử lý vượt mốc nhắc việc nội bộ 168 giờ; vui lòng liên hệ hỗ trợ nếu cần.");
        }
        return new CampaignAttentionView(false, false, "", "");
    }
}
