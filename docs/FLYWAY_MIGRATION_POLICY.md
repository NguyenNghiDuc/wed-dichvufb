# Phối hợp Flyway migrations (GrowLab, 18/10/2026)

## Xung đột phải xử lý

PR #33 (khách hàng) tạo `V10__customer_campaign_filter_index.sql`, `V11__campaign_customer_reviews.sql`, `V12__customer_contact_profiles.sql`.
PR #35 (hỗ trợ) tạo `V10__support_message_timeline.sql` đến `V15__support_owner_queue_index.sql`.
PR #37 (thanh toán thử nghiệm) tạo `V10__payment_review_indexes.sql`, `V11__deposit_history_stable_index.sql`, `V12__payment_amount_constraints.sql`.

Ba nhánh này **không được merge nguyên trạng**. Flyway sẽ từ chối version trùng khi các file hợp nhất vào một migration directory.

## Trình tự tích hợp

1. Kiểm tra HEAD `main` và toàn bộ file `src/main/resources/db/migration/V*.sql` trước khi mở PR.
2. Tích hợp tuần tự, một PR mỗi lần. Nhóm tiếp theo cần đồng bộ từ `main`, chọn những version chưa được dùng, rà soát phụ thuộc SQL và chạy PostgreSQL/Flyway CI trên tree sau cập nhật.
3. Chỉ được đổi tên migration chưa từng áp dụng ở database chung. **Không** sửa, xóa, đổi tên hay đánh lại số cho migration đã áp dụng. Nếu đã triển khai, tạo migration tiến về phía trước thay vì thay đổi checksum lịch sử.
4. Backup và xác nhận trạng thái `flyway_schema_history`, rồi mới cân nhắc chạy migrate trên staging; production yêu cầu thêm UAT và phê duyệt.

## CI

`python3 scripts/flyway_collision_check.py` chặn tên file version lỗi, version trùng và alias như V10/V10_0/V10.0.

`python3 -m unittest discover -s scripts -p 'test_flyway_collision_check.py' -v` chạy bộ kiểm thử; Java CI cũng chạy hai lệnh này trước khi build Java.

Giới hạn: checker chỉ xem *merged checkout*. Hai PR chưa merge dùng cùng version vẫn có thể xanh riêng lẻ; bắt buộc phối hợp số version và kiểm tra PR kế tiếp sau mỗi lần merge. Checker không thay thế chạy Flyway trên PostgreSQL thật và kiểm chứng checksum.
