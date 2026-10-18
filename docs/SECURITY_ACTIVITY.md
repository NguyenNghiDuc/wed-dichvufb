# W1 — Nhật ký bảo mật tài khoản

- GET `/account/security`: chỉ hiển thị tối đa 20 sự kiện mới nhất của tài khoản hiện tại, dựa trên ID trong AccountPrincipal đã xác nhận lại database.
- Ghi lại 3 loại sự kiện: đăng nhập thành công, đổi mật khẩu, thu hồi mọi phiên. Không lưu mật khẩu, token, cookie, User-Agent hay IP, nhằm giảm dữ liệu nhạy cảm.
- Ghi nhật ký đăng nhập sau xác thực bằng LoginAuditSuccessHandler; các thay đổi mật khẩu/thu hồi phiên ghi cùng giao dịch database và rollback nếu ghi sự kiện thất bại.
- Dữ liệu UTC; chưa ghi đăng nhập thất bại để tránh spam và rò rỉ tên người dùng.
- Migration PostgreSQL/Flyway `V20__account_security_activity.sql` độc lập, không sửa migration của W2–W6. Thứ tự khi tích hợp phải kiểm tra phiên bản mới của main.
- Log giới hạn 20 mục mỗi lần xem; chưa có quy trình lưu trữ/retention dài hạn và cần kế hoạch xóa theo chính sách doanh nghiệp trước khi production.
- Không phải danh sách thiết bị đang online hay chứng nhận xác thực đa yếu tố.
