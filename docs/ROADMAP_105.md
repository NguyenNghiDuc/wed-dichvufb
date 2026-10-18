# Roadmap 125 hạng mục — GrowLab

**Chính sách nghiệm thu:** Mặc định mọi hạng mục là [ ] CHƯA NGHIỆM THU, kể cả nếu đã có phiên bản thử nghiệm trong code. Chỉ đổi thành [x] khi có bằng chứng thực tế (unit/integration/security/UAT thích hợp) và review. Không tự đồng nhất “CI xanh” với “không có lỗ hổng”.

**Mỗi hạng mục yêu cầu:** (1) issue/commit/PR riêng hoặc nhóm liên quan nhỏ; (2) acceptance criteria; (3) test dương tính + sai dữ liệu + phân quyền/biên; (4) tài liệu cấu hình/migrate; (5) không có lỗ hổng mức nghiêm trọng đã biết còn mở; (6) reviewers duyệt trước merge.

**Ràng buộc:** Chỉ hỗ trợ phát triển người theo dõi thật bằng nội dung/quảng cáo hợp lệ và API cấp quyền; không bán follower giả, đăng nhập giả mạo hoặc né kiểm soát nền tảng. Các OAuth/API, VNPay và email thật cần cấu hình tài khoản thử nghiệm và credential từ chủ dự án.

**Legend:** [ ] chưa nghiệm thu; (prototype) = có code thử nghiệm chưa xác minh production. Các hạng mục dưới đây không tự nhận là đã hoàn thành.

## Bảo mật và tài khoản

- [ ] **001** Xác thực email khi đăng ký
- [ ] **002** Quên mật khẩu bằng email OTP
- [ ] **003** Xác thực hai lớp (2FA)
- [ ] **004** Đăng nhập bằng Google
- [ ] **005** Đăng nhập bằng Facebook qua OAuth
- [ ] **006** Quản lý thiết bị đăng nhập
- [ ] **007** Hiển thị lịch sử IP đăng nhập
- [ ] **008** Đăng xuất tất cả thiết bị
- [ ] **009** Cảnh báo đăng nhập bất thường
- [ ] **010** Khóa tạm thời khi đăng nhập sai quá nhiều
- [ ] **011** Quản lý phiên đăng nhập và token
- [ ] **012** Phân quyền Admin, nhân viên, đại lý, khách hàng
- [ ] **013** Ghi nhật ký hành động quan trọng
- [ ] **014** Mã hóa thông tin nhạy cảm và khóa API
- [ ] **015** Chuyển dữ liệu, mật khẩu cũ an toàn sang hệ thống mới

## Giao diện và khách hàng

- [ ] **016** Dashboard khách hàng chuyên nghiệp
- [ ] **017** Trang hồ sơ cá nhân
- [ ] **018** Thống kê đơn hàng theo thời gian thực
- [ ] **019** Tra cứu đơn bằng mã
- [ ] **020** Lịch sử sử dụng dịch vụ
- [ ] **021** Hủy yêu cầu khi chưa xử lý
- [ ] **022** Tìm kiếm và lọc đơn hàng
- [ ] **023** Đánh giá dịch vụ
- [ ] **024** Gửi yêu cầu hỗ trợ
- [ ] **025** Chat trực tiếp với nhân viên
- [ ] **026** Thông báo trên website và email
- [ ] **027** Trang hướng dẫn sử dụng và câu hỏi thường gặp

## Quản lý dịch vụ và đơn hàng

- [ ] **028** Tạo/sửa/xóa danh mục dịch vụ
- [ ] **029** Quản lý dịch vụ Facebook, Instagram, TikTok, YouTube
- [ ] **030** Quản lý gói tư vấn, nội dung và quảng cáo
- [ ] **031** Admin tự thay đổi bảng giá
- [ ] **032** Thiết lập giá riêng theo cấp khách hàng
- [ ] **033** Tính giá tự động tại server
- [ ] **034** Phê duyệt đơn trước khi triển khai
- [ ] **035** Phân công nhân viên xử lý
- [ ] **036** Theo dõi tiến độ xử lý đơn
- [ ] **037** Nhật ký chi tiết của từng đơn
- [ ] **038** Hoàn tiền hoặc điều chỉnh đơn hợp lệ
- [ ] **039** Cảnh báo đơn bị chậm, thất bại hoặc cần xử lý

## AI và phát triển mạng xã hội

- [ ] **040** AI viết caption bằng tiếng Việt và tiếng Anh
- [ ] **041** AI tạo hashtag theo chủ đề
- [ ] **042** AI gợi ý ý tưởng video TikTok
- [ ] **043** AI viết kịch bản YouTube Shorts
- [ ] **044** AI tạo lịch nội dung 7–30 ngày
- [ ] **045** AI đề xuất tiêu đề hấp dẫn
- [ ] **046** AI phân tích hiệu suất bài đăng
- [ ] **047** AI đề xuất thời điểm đăng phù hợp
- [ ] **048** AI gợi ý đối tượng quảng cáo
- [ ] **049** AI phân tích đối thủ từ dữ liệu công khai hợp lệ
- [ ] **050** AI đề xuất phương án cải thiện tương tác thật
- [ ] **051** AI kiểm tra nội dung trước khi đăng
- [ ] **052** AI tạo nhiều phiên bản nội dung để thử A/B
- [ ] **053** Quản lý lịch đăng bài từ các tài khoản đã cấp quyền
- [ ] **054** Thống kê chi phí và giới hạn sử dụng AI

## Kết nối API và tự động hóa

- [ ] **055** Facebook Pages API với quyền hợp lệ
- [ ] **056** Instagram Graph API
- [ ] **057** TikTok Developer API
- [ ] **058** YouTube Data API mở rộng
- [ ] **059** OAuth kết nối và ngắt kết nối tài khoản
- [ ] **060** Làm mới token truy cập an toàn
- [ ] **061** Đồng bộ dữ liệu và thống kê tài khoản
- [ ] **062** Hàng đợi xử lý tác vụ tự động
- [ ] **063** Retry có giới hạn khi API bị lỗi
- [ ] **064** Quản lý giới hạn API, lỗi kết nối và nhật ký đồng bộ

## Thanh toán và kinh doanh

- [ ] **065** Tích hợp VNPay thật
- [ ] **066** Tích hợp chuyển khoản QR
- [ ] **067** Xác minh giao dịch từ cổng thanh toán
- [ ] **068** Ví tiền khách hàng với sổ cái giao dịch
- [ ] **069** Lịch sử nạp và sử dụng số dư
- [ ] **070** Hoàn tiền theo quy trình
- [ ] **071** Mã giảm giá
- [ ] **072** Mã giới thiệu khách hàng
- [ ] **073** Chương trình khách hàng thân thiết
- [ ] **074** Tạo hóa đơn và biên nhận
- [ ] **075** Đối soát doanh thu hằng ngày
- [ ] **076** Phát hiện giao dịch đáng ngờ và chống cộng tiền trùng

## Trang quản trị Admin

- [ ] **077** Dashboard tổng quan doanh thu
- [ ] **078** Thống kê khách hàng mới
- [ ] **079** Quản lý tài khoản và phân quyền
- [ ] **080** Khóa/mở khóa tài khoản
- [ ] **081** Quản lý thiết bị và nhật ký đăng nhập
- [ ] **082** Quản lý đơn theo trạng thái
- [ ] **083** Phân công đơn cho nhân viên
- [ ] **084** Xử lý yêu cầu hoàn tiền
- [ ] **085** Quản lý dịch vụ và bảng giá
- [ ] **086** Quản lý thông báo toàn hệ thống
- [ ] **087** Quản lý mã giảm giá
- [ ] **088** Quản lý yêu cầu hỗ trợ
- [ ] **089** Xuất báo cáo Excel hoặc CSV
- [ ] **090** Nhật ký thay đổi dữ liệu của Admin

## Hiệu năng, triển khai và kiểm thử

- [ ] **091** Kiểm thử giao diện trên điện thoại
- [ ] **092** Kiểm thử tự động toàn bộ quy trình đăng ký → đăng nhập → tạo đơn
- [ ] **093** Kiểm thử phân quyền và truy cập chéo tài khoản
- [ ] **094** Kiểm thử đồng thời nhiều đơn hàng
- [ ] **095** Kiểm thử thanh toán và webhook giả lập
- [ ] **096** Kiểm thử API lỗi mạng, timeout, quota
- [ ] **097** Kiểm tra XSS, CSRF, SQL Injection và SSRF
- [ ] **098** Giới hạn request trên nhiều server
- [ ] **099** Tối ưu truy vấn database và phân trang
- [ ] **100** Cache dữ liệu hợp lý
- [ ] **101** Tự động backup PostgreSQL
- [ ] **102** Kiểm thử khôi phục database từ bản backup
- [ ] **103** Giám sát lỗi, thời gian phản hồi và cảnh báo
- [ ] **104** Triển khai staging và production với HTTPS
- [ ] **105** Kiểm thử tải cao, kiểm tra Docker và quy trình rollback

## Nền tảng vận hành và bảo mật

- Bảo vệ endpoint auth bằng giới hạn request theo IP/phiên, thêm security headers và kiểm thử.
- Bổ sung hủy chiến dịch khi còn chờ duyệt, ghi nhật ký và thông báo; chống truy cập chéo tài khoản.
- Phân trang đơn theo owner, tránh tải không giới hạn và lộ dữ liệu.
- Chạy Java CI, PostgreSQL thực và smoke test production JAR/Docker.
- Chưa có nghiệm thu ngoài production hay pentest độc lập.

Xem `docs/DEPLOYMENT.md` để biết những điều kiện còn thiếu trước khi vận hành chính thức.

## Đối chiếu với yêu cầu mới (2026-10-18)

Các checkbox trên là trạng thái nghiệm thu production/review, không phải khẳng định toàn bộ chưa có code. Không đánh dấu hoàn thành 105 mục chỉ vì một lần build thành công.

| Nhóm | Phần đã có trong code | Phần cần phát triển/cấu hình tiếp |
|---|---|---|
| 001–015 | BCrypt, đổi mật khẩu, role USER/STAFF/ADMIN, audit Admin. Đợt hiện tại thêm phiên đăng nhập, lịch sử IP, thu hồi từng phiên/toàn bộ, cảnh báo trình duyệt/mạng mới trên website, khóa tạm theo tài khoản và migration giữ nguyên BCrypt. | Xác minh email, email OTP, 2FA, Google/Facebook OAuth, role đại lý, mã hóa token nền tảng, nhập dữ liệu H2 cũ có đối soát. |
| 016–027 | Dashboard chiến dịch, hồ sơ, lịch sử/tìm kiếm/hủy đơn chờ, hỗ trợ, chat phân trang, thông báo website và trang hướng dẫn. | Đánh giá hoàn tất, thông báo email, tìm kiếm FAQ và cập nhật trạng thái chủ động. |
| 028–039 | Danh mục/gói tư vấn nhiều nền tảng, bảng giá admin, tính giá server, duyệt, giao việc, tiến độ/trạng thái, timeline và cảnh báo đơn chậm. Server/đơn TikTok thủ công. | Giá theo cấp, hoàn tiền/điều chỉnh có sổ cái, quản trị danh mục xóa an toàn đầy đủ. |
| 040–054 | Gợi ý AI dạng cơ bản khi có cấu hình và lịch nội dung bản nháp. | Các workflow AI riêng, quota/chi phí, phân tích từ dữ liệu đã cấp quyền, đăng theo lịch và A/B cần triển khai/kiểm thử riêng. |
| 055–064 | YouTube dữ liệu công khai có giới hạn timeout/retry và test. | OAuth chính thức, refresh token mã hóa, queue/worker, dashboard quota/sync; cần app và quyền nền tảng. |
| 065–076 | Yêu cầu nạp thủ công; webhook nội bộ có chữ ký/chống trùng và receipt. | VNPay/QR thật, ví có ledger, hoàn tiền, khuyến mãi, referral, cấp thành viên, hóa đơn và đối soát. Webhook nội bộ không phải tích hợp VNPay. |
| 077–090 | Dashboard vận hành, quản trị tài khoản/quyền/khóa, đơn/phân công/danh mục/hỗ trợ, CSV, audit; đợt này thêm xem lịch sử/phiên tài khoản. | Doanh thu đối soát từ thanh toán thật, broadcast, khuyến mãi, xử lý hoàn tiền và báo cáo Excel. |
| 091–105 | Test integration/security, H2/PostgreSQL/Flyway, viewport trình duyệt, CSRF/ownership, webhook và resilience YouTube. | Cần nghiệm thu production riêng cho rate limit phân tán, backup/restore tự động, load test, monitoring/alert, HTTPS và rollback. |

### Trình tự triển khai tiếp

1. Nền bảo mật phiên và log (đợt này); chi tiết và bằng chứng trong `ACCOUNT_SESSIONS_AND_LOGIN_SECURITY.md`.
2. Email đã cấu hình → xác minh đăng ký, quên mật khẩu OTP, sau đó 2FA và OAuth. Không ghi OTP/secret ra log hoặc yêu cầu gửi secret trong chat.
3. Luồng kinh doanh: ledger, hoàn tiền có duyệt, ưu đãi và cấp khách hàng; tích hợp sandbox trước thanh toán thật.
4. AI có hạn mức/chi phí, OAuth mạng xã hội và queue theo quyền chính thức. Không yêu cầu mật khẩu mạng xã hội.
5. Hoàn thiện UX còn thiếu, kiểm thử tải/backup/restore, staging HTTPS và nghiệm thu production.

### IX. Danh sách 106–125 theo ưu tiên của chủ dự án

Danh sách này thay thế 20 đề xuất trước. Ưu tiên và ý nghĩa mã số theo yêu cầu mới; “có code” không thay cho nghiệm thu production.

| Mã | Chức năng | Ưu tiên |
|---|---|---|
| 106 | Menu danh mục dịch vụ nhiều cấp | P1 |
| 107 | Tìm kiếm dịch vụ có gợi ý tức thì | P1 |
| 108 | Lọc dịch vụ theo nền tảng, giá, mục tiêu | P1 |
| 109 | So sánh nhiều gói dịch vụ cạnh nhau | P1 |
| 110 | Tính báo giá tức thì khi chọn gói | P0 |
| 111 | Lưu gói yêu thích | P2 |
| 112 | Lưu đơn nháp đang điền | P1 |
| 113 | Quy trình đặt dịch vụ từng bước | P0 |
| 114 | Xem lại thông tin trước khi gửi đơn | P0 |
| 115 | Trang xác nhận tạo đơn thành công | P0 |
| 116 | Timeline trực quan của từng đơn | P1 |
| 117 | Tải tài liệu, ảnh minh chứng vào yêu cầu | P1 |
| 118 | Khách hàng và nhân viên trao đổi ngay trong đơn | P1 |
| 119 | Trang dịch vụ liên quan | P2 |
| 120 | Bảng giá và FAQ riêng cho từng dịch vụ | P1 |
| 121 | Đánh giá từ đơn hoàn thành đã xác minh | P1 |
| 122 | Thanh tìm kiếm và danh mục đẹp trên mobile | P1 |
| 123 | Hỗ trợ nhanh từ trang chi tiết dịch vụ | P1 |
| 124 | SEO riêng cho từng trang dịch vụ | P1 |
| 125 | Phân tích tỷ lệ xem dịch vụ → gửi yêu cầu | P2 |

Đợt P0 tập trung 110/113/114 cho form tư vấn và 115 cho cả tư vấn/TikTok. TikTok đã có báo giá theo server/số lượng và bước chọn dịch vụ; đợt này không tự tuyên bố hoàn tất toàn bộ P1/P2. Đánh giá riêng từ đơn hoàn thành đã có code ở 023/121; công bố lên trang dịch vụ cần luồng quyền/đồng ý riêng.

### Đợt tiếp theo: đánh giá dịch vụ và FAQ có tìm kiếm

Bổ sung code và kiểm thử cho 023/027: nhận xét riêng theo đơn đã hoàn thành, một lần mỗi đơn, quản trị đọc/lọc và tìm trợ giúp tiếng Việt không dấu. Chi tiết quyền, migration V13 và nghiệm thu xem `REVIEWS_AND_SEARCHABLE_HELP.md`. Không tự đánh dấu nghiệm thu production, không thay đổi trạng thái email/OAuth/thanh toán thật.

### Đợt thông báo toàn hệ thống

Bổ sung 086, phần website của 026 và audit 090: tạo bản nháp/xem trước, nhóm nhận theo quyền và tài khoản hoạt động, xác nhận gửi tối đa 5.000 người, chống gửi lặp và lịch sử thao tác. Tích hợp trung tâm thông báo hiện có; chưa gửi email. Xem `SYSTEM_BROADCASTS.md` để biết giới hạn, migration V14 và kiểm thử giao dịch/đồng thời PostgreSQL.

### Đợt mã giảm giá đơn tư vấn

Bổ sung 071, quản trị 087 và mở rộng 033: mã VND/phần trăm, trần giảm/giá trị đơn tối thiểu, hạn dùng UTC, giới hạn tổng và mỗi tài khoản; xem trước giá server và lưu snapshot khi tạo đơn. Lượt chỉ dùng trong transaction thành công, hủy/từ chối không trả lượt. Chưa áp dụng cho TikTok hoặc ví/thanh toán thật. Xem `COUPONS_AND_CAMPAIGN_PRICING.md` cho migration V15, API và kiểm thử PostgreSQL đồng thời/rollback.
