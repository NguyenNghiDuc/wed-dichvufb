package com.example.fbservice.security;
public enum SecurityEventType {
 LOGIN_SUCCESS("Đăng nhập thành công"), LOGIN_FAILURE("Đăng nhập sai thông tin"),
 LOGIN_BLOCKED("Đăng nhập bị khóa tạm thời"), SESSION_REVOKED("Thu hồi một phiên đăng nhập"),
 LOGOUT("Đăng xuất"), ALL_SESSIONS_REVOKED("Thu hồi tất cả phiên"), PASSWORD_CHANGED("Đổi mật khẩu"),
 ACCOUNT_ROLE_CHANGED("Admin thay đổi quyền"), ACCOUNT_STATE_CHANGED("Admin thay đổi trạng thái tài khoản");
 private final String title;
 SecurityEventType(String title){this.title=title;}
 public String getTitle(){return title;}
}
