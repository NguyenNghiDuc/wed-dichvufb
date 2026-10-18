package com.example.fbservice.catalog;

import com.example.fbservice.campaign.CampaignServiceType;
import com.example.fbservice.campaign.SocialPlatform;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/** Public service pages show only configured, active consulting plans and prices. */
@Controller
public class ServiceDetailPageController {
    public record NavOption(String label, String slug, boolean selected) {}
    public record Faq(String question, String answer) {}
    private final ServiceCatalogService catalog;

    public ServiceDetailPageController(ServiceCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/dich-vu/{platformSlug}/{planSlug}")
    public String detail(@PathVariable String platformSlug, @PathVariable String planSlug,
                         Model model, Principal principal) {
        SocialPlatform platform = Arrays.stream(SocialPlatform.values())
                .filter(p -> slug(p.name()).equals(platformSlug))
                .findFirst().orElseThrow(ServiceDetailPageController::notFound);
        CampaignServiceType type = Arrays.stream(CampaignServiceType.values())
                .filter(p -> slug(p.name()).equals(planSlug))
                .findFirst().orElseThrow(ServiceDetailPageController::notFound);
        ServiceOffering selected = catalog.available().stream()
                .filter(item -> item.getCode().equals(type.name()))
                .findFirst().orElseThrow(ServiceDetailPageController::notFound);

        model.addAttribute("platform", platform);
        model.addAttribute("serviceType", type);
        model.addAttribute("platformSlug", platformSlug);
        model.addAttribute("planSlug", planSlug);
        model.addAttribute("platformName", platform.getDisplayName());
        model.addAttribute("offering", selected);
        model.addAttribute("serviceTitle", selected.getTitle() + " " + platform.getDisplayName());
        model.addAttribute("planOptions", catalog.available().stream()
                .map(item -> new NavOption(item.getTitle(), slug(item.getCode()),
                        item.getCode().equals(type.name()))).toList());
        model.addAttribute("platformOptions", Arrays.stream(SocialPlatform.values())
                .map(item -> new NavOption(item.getDisplayName(), slug(item.name()),
                        item == platform)).toList());
        model.addAttribute("signedIn", principal != null);
        model.addAttribute("description", description(type, platform.getDisplayName()));
        model.addAttribute("benefits", benefits(type));
        model.addAttribute("faqs", List.of(
                new Faq("Có cần cung cấp mật khẩu " + platform.getDisplayName() + " không?",
                        "Không. Chỉ cung cấp đường dẫn công khai và mục tiêu tư vấn. Không chia sẻ mật khẩu hoặc mã OTP."),
                new Faq("Gửi yêu cầu có tự động thanh toán không?",
                        "Không. Bạn đang gửi yêu cầu tư vấn; hệ thống chưa tự thu tiền hoặc tạo giao dịch ngân hàng."),
                new Faq("Có cam kết tăng lượt theo dõi hoặc tương tác không?",
                        "Không cam kết số liệu tăng trưởng cụ thể và không cung cấp lượt theo dõi hay tương tác giả."),
                new Faq("Theo dõi tiến độ xử lý ở đâu?",
                        "Đăng nhập và mở Chiến dịch của tôi để xem trạng thái, lịch sử xử lý và phản hồi.")
        ));
        return "service_detail";
    }

    private static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Dịch vụ không khả dụng");
    }

    private static String description(CampaignServiceType type, String platform) {
        return switch (type) {
            case PROFILE_AUDIT ->
                "Phân tích hồ sơ " + platform + ", đánh giá nội dung hiện tại và đưa ra các gợi ý cải thiện có thể thực hiện.";
            case CONTENT_PLAN ->
                "Xây dựng kế hoạch nội dung 7 ngày cho " + platform
                + " với mục tiêu, chủ đề và gợi ý định dạng rõ ràng.";
            case AD_STRATEGY ->
                "Tư vấn phương án quảng cáo " + platform
                + " phù hợp mục tiêu và ngân sách dự kiến; không tự triển khai quảng cáo.";
        };
    }

    private static List<String> benefits(CampaignServiceType type) {
        return switch (type) {
            case PROFILE_AUDIT -> List.of(
                "Đánh giá hồ sơ và thông điệp thương hiệu",
                "Gợi ý tối ưu nội dung và cách trình bày",
                "Danh sách điểm cần cải thiện để tự triển khai");
            case CONTENT_PLAN -> List.of(
                "Gợi ý chủ đề và mục tiêu từng ngày",
                "Bố cục kế hoạch nội dung 7 ngày",
                "Hướng dẫn đo lường kết quả bằng dữ liệu hợp lệ");
            case AD_STRATEGY -> List.of(
                "Làm rõ mục tiêu và đối tượng truyền thông",
                "Gợi ý phân bổ ngân sách quảng cáo",
                "Danh sách chỉ số theo dõi và rủi ro cần lưu ý");
        };
    }
}
