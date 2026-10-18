package com.example.fbservice.notification;

/** Display-only legacy notification group. Producers will eventually set explicit types. */
public enum NotificationCategory {
    ORDER("Đơn hàng"), SUPPORT("Hỗ trợ"), ACCOUNT("Tài khoản"), OTHER("Khác");
    private final String label;
    NotificationCategory(String label){this.label=label;}
    public String getLabel(){return label;}
}
