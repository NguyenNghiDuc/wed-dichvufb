# W9 GrowLab — Tiến độ 26 hạng mục (Issue #46)

**Quy tắc ba chỉ số:** 
- **Tiến độ lập trình:** Chỉ đếm code triển khai đủ luồng để chạy (🟡 hoặc ✅), dù chưa kiểm thử thành công. Hiện 19/26 — 73.08%.
- **Đã triển khai ít nhất một phần:** Đếm code đầy đủ và code một phần; bao gồm hạng mục bị chặn nhưng đã có code. Hiện 23/26 — 88.46%.
- **Tiến độ nghiệm thu:** Chỉ đếm ✅ khi test phù hợp, CI đúng SHA hoàn tất success và nghiệm thu. Hiện 0/26 — 0%.

**Phân loại bằng chứng:** 19 dòng 🟡 có code hoặc workflow kiểm thử cho hạng mục; 5 dòng 🟠 mới thực hiện một phần; 2 dòng 🔒 chưa thể thông qua release. Trong đó các dòng 🟠/#1 và #7 cùng 🔒/#4 và #26 có mã workflow/guard ít nhất một phần; **#8, #21, #25 hiện là điều tra/issue/tài liệu, không được cộng vào số có code**. Nếu phát hiện không đủ chức năng cần đổi 🟡 về 🟠, không nâng khống chỉ số.

**BẢNG TỔNG HỢP:** Đã lập trình 19/26 (73.08%); đã có code ít nhất một phần 23/26 (88.46%); nghiệm thu 0/26 (0%). Còn một phần 5; chưa làm 0; bị chặn 2; có code chưa nghiệm thu 19.

| STT | Trạng thái | Hạng mục | Bằng chứng / điều kiện còn thiếu |
|---:|:---:|---|---|
| 1 | 🟠 | Kiểm tra GitHub Actions | Đã rà soát Java CI, CodeQL, bổ sung khử chạy trùng; workflow tích hợp/HEAD gate chưa thực thi. |
| 2 | 🟡 | Java 17 và Maven | Workflow Java 17 + clean verify đã cấu hình; HEAD CI chưa kết thúc. |
| 3 | 🟡 | PostgreSQL 16 | CI service PostgreSQL 16 và test race đã viết; chưa có kết quả HEAD. |
| 4 | 🔒 | Flyway migration kết hợp | W2 #33, W3 #35, W6 #37 còn trùng version; #41, cần W7. |
| 5 | 🟡 | Production JAR smoke test | Java CI có khởi động JAR profile prod; chưa đạt trên HEAD. |
| 6 | 🟡 | Docker image/runtime | CI build và docker run/health-check đã lập trình; chưa có kết quả HEAD. |
| 7 | 🟠 | CodeQL và security findings | Đã kiểm tra log, sửa lịch kích hoạt lặp; lỗi phân tích CSRF vẫn tồn tại (#13). |
| 8 | 🟠 | Theo dõi CSRF Issue #13 | Đã báo lỗi CodeQL path graph và đính kèm run; chưa xử lý gốc. |
| 9 | 🟡 | E2E đăng ký và đăng nhập | W9CustomerJourneyIntegrationTest dùng session đăng nhập thật; chờ CI. |
| 10 | 🟡 | E2E tạo chiến dịch | Có create API và kiểm tra ownership/status; chờ CI. |
| 11 | 🟡 | Theo dõi và hủy đơn | History/status và duplicate cancel test; chờ CI. |
| 12 | 🟡 | Tạo ticket hỗ trợ | Có test tạo, xem, reply trái quyền và limit; chờ CI. |
| 13 | 🟡 | Quyền USER/STAFF/ADMIN | W9SessionRolesAndDuplicateTest; chờ CI. |
| 14 | 🟡 | Truy cập chéo tài khoản | Kiểm tra owner/outsider đơn và ticket; chờ CI. |
| 15 | 🟡 | CSRF, XSS, dữ liệu sai | Kiểm thử CSRF thiếu, HTML escape, bad JSON, input; chờ CI. |
| 16 | 🟡 | API trả lỗi | Bad JSON, page quá giới hạn, ticket rate 429; chờ CI. |
| 17 | 🟡 | Request trùng | Hủy lặp không tạo event và ticket rate limit; chờ CI. |
| 18 | 🟡 | Đồng thời/race condition | PostgresW9CancellationRaceTest, 2 luồng; chờ CI. |
| 19 | 🟡 | Session và đăng xuất | Test logout CSRF/session invalidation; chờ CI. |
| 20 | 🟡 | JaCoCo coverage | Đã thêm kiểm tra XML và line coverage trong CI; chưa đo thực tế HEAD. |
| 21 | 🟠 | Báo lỗi test đúng nhóm | Đã báo W1/W3/W6/W7 và Issue #13; đang theo dõi phản hồi. |
| 22 | 🟡 | Phát hiện PR sửa cùng file | Có workflow `w9-pr-overlap.yml` liệt kê toàn bộ file trùng giữa các PR đang mở và đưa cảnh báo lên Actions Summary; chờ CI HEAD. |
| 23 | 🟡 | Xác minh CI đúng HEAD | `w9-head-quality-gate.yml` kiểm tra đúng SHA hiện hành, từ chối queued/cancelled/draft; chờ CI. |
| 24 | 🟡 | Release quality gate | Có Flyway guard, JaCoCo XML gate và HEAD gate, đang chờ CI; chưa chứng minh staging release đạt. |
| 25 | 🟠 | Báo cáo QA/checklist | Tài liệu QA và bảng theo dõi đã viết; còn staging/manual signoff. |
| 26 | 🔒 | Integration giữa nhiều PR | Đã viết workflow thử merge SHA chính xác; #41 và workflow chưa mặc định chặn. |

## Nguồn bằng chứng

- Nhánh W9: `feature/w9-cross-module-qa-issue46`, draft PR https://github.com/NguyenNghiDuc/wed-dichvufb/pull/48. Luôn kiểm tra lại `head.sha` trước khi trích dẫn CI.
- CI: https://github.com/NguyenNghiDuc/wed-dichvufb/actions ; Java CI, CodeQL và PostgreSQL/Flyway/JAR/Docker của HEAD chờ kết quả thật.
- Công cụ đã tạo: `W9CustomerJourneyIntegrationTest`, `PostgresW9CancellationRaceTest`, `W9SessionRolesAndDuplicateTest`, `W9SupportRateLimitIntegrationTest`; workflow `java-ci.yml`, `w9-integration-rehearsal.yml`, `w9-head-quality-gate.yml`, `w9-pr-overlap.yml`.
- CodeQL CSRF: https://github.com/NguyenNghiDuc/wed-dichvufb/issues/13; lỗi graph xác minh tại https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/39093099906, dù run kết luận success.
- Xung đột migration: https://github.com/NguyenNghiDuc/wed-dichvufb/issues/41; PR #33, #35, #37; PR #44 chỉ là đề xuất renumber tách rời.
- Chưa merge, chưa triển khai production, chưa thao tác DB thật. Runtime hiện tại không có Maven/Docker/Java 17, checkout GitHub trực tiếp từ container bị lỗi DNS; kết quả CLI cục bộ không dùng để đánh dấu nghiệm thu.

## Cách cập nhật sau mỗi vòng

Khi test/CI/PR có SHA mới, cập nhật ma trận theo từng dòng và đăng bản sao vào Issue #46. Trạng thái ✅ chỉ xuất hiện khi có kết quả CI thành công đúng HEAD, kiểm thử đạt và nghiệm thu phù hợp. Nếu tăng phạm vi, tăng mẫu số Y và giữ toàn bộ các dòng cũ.

## Chỉnh lịch chạy CI (18/10/2026)

Workflow `java-ci.yml` và `codeql.yml` chỉ chạy `push` trên main và `pull_request` trên nhánh đích; `concurrency` tự hủy lượt PR cũ khi có commit mới. Các run `cancelled` không bao giờ được tính là PASS. `w9-pr-overlap.yml` chỉ xuất cảnh báo PR sửa chung file, không tự coi overlap là conflict hay tự merge.
