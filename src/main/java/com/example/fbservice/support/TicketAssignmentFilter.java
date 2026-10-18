package com.example.fbservice.support;

/** Fixed queue views; clients never supply an arbitrary employee username. */
public enum TicketAssignmentFilter {
    ALL("Tất cả"),
    MINE("Tôi phụ trách"),
    UNASSIGNED("Chưa ai nhận");

    private final String title;
    TicketAssignmentFilter(String title) { this.title = title; }
    public String getTitle() { return title; }
}
