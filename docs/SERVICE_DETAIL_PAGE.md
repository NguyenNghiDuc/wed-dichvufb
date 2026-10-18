# GrowLab — Trang chi tiết dịch vụ trắng–cam

Trang lấy cảm hứng từ cấu trúc trang Like3s người dùng cung cấp, không sao chép logo, dữ liệu đánh giá, con số thống kê hoặc cam kết tăng follower. Không bán tương tác giả.

## Đường dẫn

- /dich-vu/tiktok/profile-audit (được liên kết từ trang chủ)
- /dich-vu/facebook/content-plan
- /dich-vu/instagram/ad-strategy
- Kết hợp 4 nền tảng với 3 loại dịch vụ có trong catalog.

## Chức năng

Hero, breadcrumb, thanh điều hướng mục, thông tin, mô tả, FAQ, quy trình, khu vực đánh giá trung thực, sidebar thay đổi nền tảng/gói và gửi yêu cầu.

Giá đọc trực tiếp từ ServiceCatalogService, chỉ phục vụ gói đang enabled. Người chưa đăng nhập được dẫn tới trang login. Người đã đăng nhập có form POST /campaigns kèm CSRF; CampaignOrderService tiếp tục là nơi kiểm tra URL, quyền owner và tính giá. Không có số lượng follower/server, thanh toán tự động, OAuth hoặc nhận mật khẩu.

CSS được scope dưới body.service-detail trong style.css vì Spring Security main hiện chỉ cho phép khách truy cập /style.css. W3 đang có thay đổi trên cùng file; kiểm tra khi merge.

## Kiểm thử

mvn -Dtest=ServiceDetailPageIntegrationTest test

Bài test kiểm tra 12 tổ hợp, giá do server xác định, quyền owner, CSRF, route invalid, CSS và dữ liệu marketing không giả tạo. Full Java 17 CI/PostgreSQL/Docker/CodeQL và UAT mobile cần xác minh trước merge.

## Làm mới trang theo tham chiếu SocialBoost Asia (18/10/2026)

Trang tham khảo: https://socialboost.asia/vi-vn/tiktok/tang-follow. Chỉ sử dụng cảm hứng về cấu trúc UI: menu danh mục, hero minh họa, bảng lựa chọn có bước, giá gói, mục giải thích, hướng dẫn, tư vấn, FAQ và đánh giá.

Phiên bản GrowLab có **thiết kế riêng**: nền trắng ngà, hero xanh đen/cam, icon/đồ họa CSS riêng, sidebar 4 nền tảng, bộ chọn 3 gói tư vấn, khung 4 bước (nền tảng → gói → link/mô tả → gửi yêu cầu) và 6 mục nội dung SEO có liên kết đầu trang. Không sao chép tên SocialBoost, logo, hình robot, danh sách máy chủ theo lượt follow, thống kê 50.000+ đơn, điểm 4,9 sao hoặc review của trang khác.

**Pricing / backend:** Model `priceRows` được dựng từ cùng `catalog.available()` trên request (không hardcode giá); gói đã vô hiệu hóa không được render. Chỉ có 4 nền tảng và 3 tư vấn gốc của GrowLab; đổi lựa chọn qua GET. Khi đã đăng nhập, form POST `/campaigns` CSRF giữ nguyên; khách chưa đăng nhập được chuyển tới login, không nhận giao dịch thật. Không có chức năng thay số follow hoặc thanh toán qua ví.

**Responsive / accessibility:** Sidebar ở desktop trở thành thanh danh mục cuộn ngang trên thiết bị nhỏ; hero và order card xếp dọc; giá table cuộn ngang; có skip link, label input, FAQ details và focus-visible. CSS chỉ thêm các selector `grow-*` trong `style.css`, không dùng JS ngoài và không thay đổi styles chung.

**Kiểm tra:** chạy `mvn -Dtest=ServiceDetailPageIntegrationTest test` (5 tests, gồm một test UI/pricing mới) trên Java 17. Sau đó chạy PostgreSQL/Flyway/JAR/Docker CI và CodeQL trên exact head commit. Cần kiểm tra giao diện thực tế trên Codespaces và mobile, phản hồi trước khi merge. Không đánh dấu nghiệm thu khi CI còn queued/cancelled.

**Lưu ý xung đột:** `style.css` cùng W3 PR #35, `WebSecurityConfig.java` cùng W1 PR #22, và W2 PR #33 cùng sở hữu luồng customer campaign. Khi merge cần giữ cả hai nhánh, không ghi đè nguyên file.
