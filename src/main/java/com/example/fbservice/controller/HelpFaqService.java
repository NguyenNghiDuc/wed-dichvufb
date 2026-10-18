package com.example.fbservice.controller;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Curated public FAQ. No customer data, credentials or database access. */
@Service
public class HelpFaqService {
    public record Topic(String key, String label) {}

    public static final List<Topic> TOPICS = List.of(
        new Topic("all", "Tất cả"),
        new Topic("account", "Tài khoản"),
        new Topic("campaign", "Chiến dịch"),
        new Topic("content", "Nội dung"),
        new Topic("payment", "Thanh toán"),
        new Topic("support", "Hỗ trợ")
    );

    private static final List<HelpFaq> FAQS = List.of(
        new HelpFaq("campaign", "Đặt chiến dịch như thế nào?",
            "Đăng nhập, chọn Tạo chiến dịch, chọn gói tư vấn và nhập đường dẫn HTTPS của tài khoản đúng nền tảng."),
        new HelpFaq("campaign", "Vì sao chiến dịch đang chờ duyệt?",
            "Chiến dịch mới cần được nhân viên kiểm tra trước khi xử lý. Bạn xem trạng thái trong Chiến dịch của tôi."),
        new HelpFaq("campaign", "Chiến dịch có chạy tự động không?",
            "Không. Đơn đang được xem xét và xử lý thủ công; GrowLab không tự tăng lượt theo dõi."),
        new HelpFaq("campaign", "Hủy đơn được không?",
            "Bạn có thể hủy chiến dịch khi đơn còn ở trạng thái Chờ duyệt (PENDING_REVIEW)."),
        new HelpFaq("campaign", "Tôi tải danh sách chiến dịch ở đâu?",
            "Vào Chiến dịch của tôi, chọn bộ lọc và bấm Xuất CSV theo bộ lọc. Chỉ xuất chiến dịch của tài khoản bạn."),
        new HelpFaq("campaign", "Báo giá có phải đã thanh toán không?",
            "Không. Giá hiển thị là báo giá cho yêu cầu, không phải bằng chứng thanh toán thành công."),
        new HelpFaq("content", "GrowLab có tăng lượt theo dõi ảo không?",
            "Không. Dịch vụ tập trung vào nội dung hữu ích, tối ưu hồ sơ và tư vấn quảng cáo hợp lệ."),
        new HelpFaq("content", "Lịch nội dung có tự đăng bài không?",
            "Không. Lịch chỉ lưu bản nháp và thời gian dự kiến; không tự đăng lên mạng xã hội."),
        new HelpFaq("content", "Tôi có thể sửa hoặc bỏ lịch nội dung không?",
            "Có. Mở Lịch nội dung, chọn Sửa để thay đổi bài viết hoặc bỏ giờ dự kiến và lưu thành bản nháp."),
        new HelpFaq("payment", "Chức năng nạp tiền đã hoạt động chưa?",
            "Chưa có cổng thanh toán thật trong bản thử nghiệm. Không chuyển tiền dựa trên form thử nghiệm."),
        new HelpFaq("account", "Có cần tài khoản để tạo chiến dịch không?",
            "Có. Bạn cần đăng ký và đăng nhập để tạo chiến dịch, theo dõi lịch sử và gửi yêu cầu hỗ trợ."),
        new HelpFaq("support", "Liên hệ hỗ trợ ở đâu?",
            "Sau khi đăng nhập, vào mục Hỗ trợ và tạo phiếu yêu cầu. Bạn có thể xem lại lịch sử phản hồi.")
    );

    public List<Topic> topics() {
        return TOPICS;
    }

    public List<HelpFaq> search(String rawQuery, String rawCategory) {
        String query = rawQuery == null ? "" : rawQuery.strip();
        String category = rawCategory == null || rawCategory.isBlank() ? "all" : rawCategory.strip();
        if (query.length() > 80 || category.length() > 20 ||
            TOPICS.stream().noneMatch(topic -> topic.key().equals(category))) {
            throw new IllegalArgumentException("Bộ lọc trợ giúp không hợp lệ.");
        }
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return FAQS.stream().filter(faq -> category.equals("all") || faq.category().equals(category))
                .toList();
        }
        String[] terms = normalized.split("\\s+");
        return FAQS.stream()
            .filter(faq -> category.equals("all") || faq.category().equals(category))
            .filter(faq -> {
                String searchable = normalize(faq.question() + " " + faq.answer());
                for (String term : terms) {
                    if (!searchable.contains(term)) return false;
                }
                return true;
            })
            .toList();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT).replace('đ', 'd'),
                Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replaceAll("\\s+", " ")
            .strip();
    }
}
