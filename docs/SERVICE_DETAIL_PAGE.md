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
