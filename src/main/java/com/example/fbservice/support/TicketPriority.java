package com.example.fbservice.support;

/** Staff-selected urgency, distinct from the lifecycle status. */
public enum TicketPriority {
    LOW("Thấp"), NORMAL("Bình thường"), HIGH("Cao"), URGENT("Khẩn cấp");

    private final String title;
    TicketPriority(String title) { this.title = title; }
    public String getTitle() { return title; }
}
