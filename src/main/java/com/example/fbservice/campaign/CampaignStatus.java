package com.example.fbservice.campaign;

public enum CampaignStatus {
    PENDING_REVIEW("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    IN_PROGRESS("Đang thực hiện"),
    COMPLETED("Hoàn thành"),
    REJECTED("Từ chối");

    private final String displayName;

    CampaignStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
