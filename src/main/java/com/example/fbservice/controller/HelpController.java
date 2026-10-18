package com.example.fbservice.controller;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public, read-only help center. FAQs are curated in code; user input never reaches SQL or HTML markup.
 */
@Controller
public class HelpController {
    private static final int MAX_QUERY_LENGTH = 80;
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Set<String> CATEGORIES =
        Set.of("all", "account", "campaign", "content", "payment", "support");

    private static final List<FaqItem> FAQS = List.of(
        new FaqItem("account", "Tài khoản", "Làm sao tạo tài khoản?",
            "Chọn Đăng ký, nhập thông tin theo biểu mẫu và đăng nhập để sử dụng các chức năng dành cho khách hàng."),
        new FaqItem("account", "Tài khoản", "Tôi quên mật khẩu thì làm gì?",
            "Chức năng khôi phục mật khẩu qua email chưa được xác nhận hoạt động. Không gửi mật khẩu hay mã OTP cho bất kỳ ai; hãy liên hệ hỗ trợ qua kênh chính thức."),
        new FaqItem("account", "Tài khoản", "Tại sao tôi phải đăng nhập để xem đơn?",
            "Đơn hàng và lịch sử chiến dịch là dữ liệu riêng tư, chỉ người dùng được xác thực và nhân sự có quyền phù hợp mới được truy cập."),
        new FaqItem("campaign", "Chiến dịch", "Làm sao tạo chiến dịch mới?",
            "Đăng nhập, mở trang Chiến dịch và chọn Tạo chiến dịch. Chọn gói tư vấn, cung cấp đường dẫn hợp lệ rồi gửi yêu cầu để nhân viên xem xét."),
        new FaqItem("campaign", "Chiến dịch", "Tôi xem tiến độ đơn hàng ở đâu?",
            "Mở danh sách Chiến dịch trong tài khoản để xem trạng thái và lịch sử xử lý của yêu cầu."),
        new FaqItem("campaign", "Chiến dịch", "Có thể hủy đơn hàng không?",
            "Bạn có thể yêu cầu hủy khi chiến dịch vẫn ở trạng thái chờ duyệt. Các giai đoạn khác cần trao đổi với bộ phận hỗ trợ."),
        new FaqItem("campaign", "Chiến dịch", "Nếu chiến dịch bị từ chối thì sao?",
            "Hãy kiểm tra trạng thái và lý do được cung cấp, sau đó gửi yêu cầu hỗ trợ nếu cần được giải thích thêm."),
        new FaqItem("content", "Nội dung", "GrowLab có bán người theo dõi ảo không?",
            "Không. GrowLab hướng tới tăng trưởng thật thông qua tư vấn nội dung, tối ưu hồ sơ và quảng cáo hợp lệ."),
        new FaqItem("content", "Nội dung", "Hệ thống có tự đăng bài lên mạng xã hội không?",
            "Chưa có chức năng tự đăng bài trên tài khoản bên thứ ba khi chưa được cấp quyền hợp lệ. Nội dung và lịch nháp chỉ để lên kế hoạch."),
        new FaqItem("content", "Nội dung", "Tôi có thể chuẩn bị lịch nội dung không?",
            "Bạn có thể dùng khu vực nháp và lịch nội dung nếu tính năng đã được bật cho tài khoản. Kiểm tra lại nội dung trước khi đăng."),
        new FaqItem("payment", "Thanh toán", "Tôi có thể nạp tiền thật trên GrowLab chưa?",
            "Chưa. Chức năng thanh toán vẫn đang trong giai đoạn kiểm thử nội bộ. Không chuyển tiền dựa vào biểu mẫu hoặc mã thử nghiệm."),
        new FaqItem("payment", "Thanh toán", "Thông báo thanh toán thử nghiệm có phải biên nhận thật không?",
            "Không. Trạng thái và dữ liệu thanh toán trong môi trường thử nghiệm không phải xác nhận đã nhận tiền thật."),
        new FaqItem("support", "Hỗ trợ", "Tôi gửi yêu cầu hỗ trợ ở đâu?",
            "Đăng nhập rồi mở Trung tâm hỗ trợ để tạo phiếu, trình bày vấn đề và theo dõi phản hồi."),
        new FaqItem("support", "Hỗ trợ", "Tôi có nên gửi mật khẩu hoặc mã OTP qua chat?",
            "Không. Không bao giờ chia sẻ mật khẩu, OTP, khóa API hoặc thông tin thẻ qua biểu mẫu hỗ trợ hay tin nhắn.")
    );

    @GetMapping("/help")
    public String help(@RequestParam(name = "q", defaultValue = "") String query,
                       @RequestParam(name = "category", defaultValue = "all") String category,
                       Model model) {
        String trimmed = query.strip();
        if (trimmed.length() > MAX_QUERY_LENGTH || !CATEGORIES.contains(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid help filters");
        }

        String normalized = normalize(trimmed);
        String[] terms = normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
        List<FaqItem> results = FAQS.stream()
            .filter(faq -> category.equals("all") || faq.getCategoryKey().equals(category))
            .filter(faq -> {
                String haystack = normalize(faq.getQuestion() + " " + faq.getAnswer());
                for (String term : terms) {
                    if (!haystack.contains(term)) {
                        return false;
                    }
                }
                return true;
            })
            .toList();

        model.addAttribute("faqs", results);
        model.addAttribute("count", results.size());
        model.addAttribute("query", trimmed);
        model.addAttribute("selectedCategory", category);
        return "help";
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return MARKS.matcher(decomposed).replaceAll("")
            .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }

    public static final class FaqItem {
        private final String categoryKey;
        private final String categoryLabel;
        private final String question;
        private final String answer;

        private FaqItem(String categoryKey, String categoryLabel, String question, String answer) {
            this.categoryKey = categoryKey;
            this.categoryLabel = categoryLabel;
            this.question = question;
            this.answer = answer;
        }

        public String getCategoryKey() { return categoryKey; }
        public String getCategoryLabel() { return categoryLabel; }
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
    }
}
