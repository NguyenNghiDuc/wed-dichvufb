package com.example.fbservice.campaign;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ContentSuggestionController {
    // Deterministic local templates only. No external AI provider is enabled.
    @GetMapping("/services")
    public CampaignServiceType[] services() {
        return CampaignServiceType.values();
    }

    @GetMapping("/content-suggestions")
    public Map<String, Object> suggest(@RequestParam SocialPlatform platform,
                                       @RequestParam String topic) {
        String clean = topic.strip();
        if (clean.length() < 3 || clean.length() > 100) {
            throw new IllegalArgumentException("Chủ đề cần 3 đến 100 ký tự.");
        }
        return Map.of(
            "mode", "TEMPLATE_ONLY",
            "platform", platform.name(),
            "suggestions", List.of(
                "3 điều bạn nên biết về " + clean + " — lưu lại để xem sau!",
                "Bạn đã thử cách này với " + clean + " chưa? Chia sẻ kinh nghiệm ở bình luận.",
                "Hướng dẫn nhanh: bắt đầu với " + clean + " trong 3 bước."
            )
        );
    }
}
