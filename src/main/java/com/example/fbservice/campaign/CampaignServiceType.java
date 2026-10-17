package com.example.fbservice.campaign;

public enum CampaignServiceType {
    PROFILE_AUDIT("Đánh giá & tối ưu hồ sơ", 79000L),
    CONTENT_PLAN("Lên kế hoạch nội dung 7 ngày", 149000L),
    AD_STRATEGY("Tư vấn chiến dịch quảng cáo", 299000L);

    private final String displayName;
    private final long priceVnd;

    CampaignServiceType(String displayName, long priceVnd) {
        this.displayName = displayName;
        this.priceVnd = priceVnd;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getPriceVnd() {
        return priceVnd;
    }
}
