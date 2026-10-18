package com.example.fbservice.broadcast;
public enum BroadcastStatus {
 DRAFT("Bản nháp"),SENT("Đã gửi"),CANCELLED("Đã hủy");
 private final String title;BroadcastStatus(String title){this.title=title;}public String getTitle(){return title;}
}
