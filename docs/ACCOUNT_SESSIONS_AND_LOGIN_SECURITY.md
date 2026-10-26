# Phiên đăng nhập và lịch sử bảo mật

Phạm vi triển khai: roadmap 006, 007, 008, 009, 010, một phần 011/013/015 và màn hình Admin 081. Không thay thế 2FA, không tự kết nối email hay OAuth.

## Hành vi

- Đăng nhập bằng Spring Security/BCrypt; luân chuyển session ID khi xác thực. Cookie HttpOnly/SameSite=Lax; production bật Secure, chỉ dùng cookie, idle timeout 30 phút.
- `/profile/security`: tối đa 10 phiên hoạt động, trình duyệt (User-Agent), IP lúc đăng nhập, thời gian đăng nhập và hoạt động gần nhất. Khách hàng chỉ xem dữ liệu của mình. Phiên thứ 11 thu hồi phiên cũ nhất; thu hồi có hiệu lực ở request tiếp theo.
- POST `/profile/security/sessions/{id}/revoke`: CSRF và mật khẩu hiện tại, giới hạn 5 lần/15 phút theo tài khoản trên mỗi instance. Phiên của khách khác trả 404. Thu hồi phiên hiện tại đăng xuất ngay.
- Đăng xuất, đổi mật khẩu và đăng xuất toàn bộ phiên cập nhật bản ghi phiên/lịch sử. Đổi mật khẩu/thu hồi tất cả tăng `auth_version`; các phiên cũ không thể dùng tiếp. Phiên chưa được theo dõi trước khi triển khai phải đăng nhập lại.
- Lịch sử đăng nhập và sự kiện bảo mật 20 dòng/trang, sắp xếp thời gian + ID. Không ghi mật khẩu, cookie, session ID hoặc khóa API. Database chỉ lưu SHA-256 session ID; giá trị này cũng không gửi tới giao diện.
- Khi đã có lịch sử đăng nhập và thấy cặp IP/User-Agent mới, tạo thông báo trên website. Đây là tín hiệu thay đổi trình duyệt/mạng, **không** xác định thiết bị vật lý hoặc khẳng định tài khoản bị xâm nhập; không có geolocation hay cảnh báo email trong đợt này.
- Sau 5 lần sai mật khẩu trong cửa sổ 15 phút, khóa đăng nhập mới 15 phút. Sai từ các IP khác nhau dùng chung bộ đếm trong PostgreSQL. Request trong thời gian khóa không kéo dài khóa. Đăng nhập đúng sau hết hạn đặt lại bộ đếm; phiên đã xác thực trước đó vẫn dùng được để đổi mật khẩu/thu hồi. Khóa thủ công của Admin vẫn là trạng thái độc lập.
- Admin đổi quyền hoặc khóa/mở khóa sẽ thu hồi các phiên cũ và tăng `auth_version`, tránh phiên lấy lại quyền khi role được đổi trở lại. Audit Admin vẫn ghi người thực hiện.
- `/admin/users/{id}/security`: chỉ ADMIN xem phiên/lịch sử qua liên kết trong bảng tài khoản; STAFF và khách hàng bị từ chối. Admin không được đọc token phiên hoặc nhập vai khách hàng qua màn hình này.

## Dữ liệu và vận hành

V12 bổ sung ba cột đếm/khóa vào `users`, bảng `account_sessions`, `account_security_events` và index theo chủ tài khoản. Migration không đổi hash mật khẩu hoặc xóa dữ liệu đơn hàng; tăng phiên bản xác thực để thu hồi phiên trước triển khai. H2 development dùng schema update; PostgreSQL production dùng Flyway rồi Hibernate validate. Drain các instance phiên bản cũ trước migration/deploy; không chạy đồng thời bản trước và sau V12 để tránh node cũ bỏ qua chính sách khóa đăng nhập. Sao lưu trước khi triển khai; không chỉnh sửa migration cũ hay hạ phiên bản ứng dụng trong khi còn phiên đang hoạt động.

Giờ ghi theo UTC. User-Agent được giới hạn 200 ký tự, loại ký tự điều khiển và escape khi hiển thị. IP lấy từ peer TCP. `server.forward-headers-strategy=none` tránh tin header X-Forwarded-For do khách gửi. Nếu có reverse proxy/CDN, mặc định IP có thể là IP proxy: chỉ bật forwarded headers sau khi cấu hình proxy tin cậy, chặn kết nối trực tiếp và kiểm tra lại. Cookie Secure yêu cầu HTTPS production.

Danh sách phiên lọc theo hạn dùng, trạng thái thu hồi và phiên bản xác thực. Lịch sử được giữ trong database; vận hành cần xác định thời hạn lưu IP/User-Agent và quy trình xóa/ẩn danh theo chính sách riêng trước production. Chưa có tác vụ retention tự động.

Khóa theo tài khoản dùng transaction và row lock nên chia sẻ giữa các instance. Giới hạn theo IP và giới hạn form vẫn dùng bộ nhớ từng instance. Session HTTP hiện nằm tại từng instance: triển khai nhiều instance cần sticky routing hoặc Spring Session dùng Redis/JDBC; chưa tự nhận hoàn tất roadmap 098.

## Kiểm chứng

- `AccountSessionSecurityIntegrationTest`: đăng nhập/luân chuyển session ID, chỉ lưu hash, chống XSS/giả forwarded IP, thu hồi phiên khác/hiện tại, CSRF, mật khẩu sai, chống truy cập chéo, đăng xuất, giới hạn 10 phiên, cảnh báo mới, khóa/hết khóa, phân trang và phân quyền Admin.
- `AccountSettingsIntegrationTest`: dùng đăng nhập thực cho đổi mật khẩu/thu hồi toàn bộ; xác nhận principal cũ bị chặn.
- `PostgresAccountSessionSecurityTest`: 5 lỗi đăng nhập đồng thời không mất bộ đếm; nâng schema V11→V12 giữ nguyên BCrypt và dữ liệu dịch vụ, thu hồi phiên cũ.

Chạy theo Java 17 và hướng dẫn build của repository. Trình duyệt nghiệm thu phải kiểm tra thêm hai context đăng nhập đồng thời, thu hồi phiên, hộp thoại xác nhận, đăng xuất, lịch sử và kích thước 320/390/768 px.

Kết quả cloud 2026-10-18: `clean verify` 121 test qua, không skip; bản bổ sung kiểm tra đăng nhập lại và vòng đổi role chạy 16 test liên quan thành công. Sau chỉnh thứ tự lock đăng xuất, chạy lại 12 test phiên/PostgreSQL thành công. Migration guard 8 test qua. Kiểm tra Chromium hai context khách hàng và một Admin: thu hồi phiên xa/hiện tại, giữ form lỗi, cảnh báo website, lịch sử Admin và 8 lượt kiểm tra viewport (320/390/768/1440 px) đều qua, không có lỗi JavaScript. Smoke test app khởi động lại: health UP, đăng ký/đăng nhập và CSRF hoạt động. Đây là bằng chứng ở cloud/test database, chưa phải nghiệm thu production.
