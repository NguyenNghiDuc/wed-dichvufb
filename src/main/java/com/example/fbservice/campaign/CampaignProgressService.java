package com.example.fbservice.campaign;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CampaignProgressService {
    private static final List<String> LABELS = List.of(
        "Chờ duyệt", "Đã duyệt", "Đang thực hiện", "Hoàn thành");

    public CampaignProgressView view(CampaignOrder order) {
        CampaignStatus status = order.getStatus();
        boolean closed = status == CampaignStatus.CANCELLED || status == CampaignStatus.REJECTED;
        int index = switch (status) {
            case PENDING_REVIEW -> 0;
            case APPROVED -> 1;
            case IN_PROGRESS -> 2;
            case COMPLETED -> 3;
            case REJECTED, CANCELLED -> -1;
        };
        int percent = switch (index) {
            case 1 -> 33;
            case 2 -> 67;
            case 3 -> 100;
            default -> 0;
        };
        List<CampaignProgressView.Step> stages = new ArrayList<>();
        for (int i = 0; i < LABELS.size(); i++) {
            stages.add(new CampaignProgressView.Step(LABELS.get(i),
                !closed && index > i, !closed && index == i));
        }
        return new CampaignProgressView(status, percent, closed, List.copyOf(stages));
    }
}
