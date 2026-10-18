package com.example.fbservice.integration;

import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Compliance-readiness reference, NOT proof of connected user accounts.
 * Live OAuth/storage/posting requires reviewed scopes, consent and owner setup.
 */
@RestController
@RequestMapping("/api/v1/integrations")
public class PlatformAccessRequirementsController {
    public record Requirement(String platform, boolean accountConnected,
        boolean publishingEnabled, boolean ownerConsentRequired,
        List<String> exampleReadScopes, List<String> examplePublishScopes,
        String officialDocs, String warning) {}

    @GetMapping("/requirements")
    public List<Requirement> requirements() {
        String reminder="Chỉ kết nối tài khoản chủ sở hữu đã ủy quyền hợp lệ. "
            +"Quyền cụ thể tùy endpoint và mức xét duyệt; chưa có OAuth hoặc tự đăng.";
        return List.of(
            new Requirement("FACEBOOK",false,false,true,
                List.of("pages_show_list","pages_read_engagement"),
                List.of("pages_manage_posts"),
                "https://developers.facebook.com/docs/pages-api/",
                reminder),
            new Requirement("INSTAGRAM",false,false,true,
                List.of("instagram_business_basic"),
                List.of("instagram_business_content_publish"),
                "https://www.postman.com/meta/instagram/folder/6raa77c/instagram-api-with-instagram-login",
                reminder+" Instagram Login dành cho tài khoản chuyên nghiệp, không phải mọi tài khoản cá nhân."),
            new Requirement("TIKTOK",false,false,true,
                List.of("user.info.basic","video.list"),
                List.of("video.publish"),
                "https://developers.tiktok.com/docs/en/content-posting-api-get-started",
                reminder+" Video publish cần app review, scope approval và consent."),
            new Requirement("YOUTUBE",false,false,true,
                List.of("API key (public channels)","youtube.readonly (private data, OAuth)"),
                List.of("youtube.upload"),
                "https://developers.google.com/youtube/v3/docs",
                reminder+" Trạng thái public-read API key xem /api/v1/integrations.")
        );
    }
}
