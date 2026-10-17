package com.example.fbservice.support;

/** Explicit ticket workflow to prevent arbitrary client-side status changes. */
public enum TicketStatus {
    OPEN("Đang chờ hỗ trợ"),
    ANSWERED("Đã phản hồi"),
    CLOSED("Đã đóng");

    private final String title;
    TicketStatus(String title) { this.title = title; }
    public String getTitle() { return title; }
}
