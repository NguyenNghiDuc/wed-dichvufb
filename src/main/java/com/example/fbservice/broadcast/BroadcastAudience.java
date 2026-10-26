package com.example.fbservice.broadcast;
public enum BroadcastAudience {
 ALL("Tất cả tài khoản đang hoạt động"),USER("Khách hàng"),STAFF("Nhân viên"),ADMIN("Quản trị viên");
 private final String title;BroadcastAudience(String title){this.title=title;}public String getTitle(){return title;}
}
