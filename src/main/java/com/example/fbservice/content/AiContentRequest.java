package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import jakarta.validation.constraints.*;

/** Optional fields keep the legacy /caption JSON request backward compatible. */
public record AiContentRequest(
        @NotNull SocialPlatform platform,
        @NotBlank @Size(min=3,max=150) String topic,
        @NotBlank @Size(min=3,max=100) String audience,
        AiContentTask task,
        AiContentLanguage language,
        @Min(7) @Max(30) Integer days,
        @Size(max=3000) String existingContent) {

    public AiContentRequest(SocialPlatform platform, String topic, String audience) {
        this(platform,topic,audience,null,null,null,null);
    }
    /** Enforce platform-specific formats without guessing publishing permissions. */
    @AssertTrue(message="Ý tưởng TikTok chỉ áp dụng cho nền tảng TikTok; Shorts chỉ áp dụng cho YouTube.")
    public boolean isTaskPlatformCompatible() {
        return platform==null || task==null ||
            (task!=AiContentTask.TIKTOK_IDEAS || platform==SocialPlatform.TIKTOK) &&
            (task!=AiContentTask.YOUTUBE_SHORTS || platform==SocialPlatform.YOUTUBE) &&
            (task!=AiContentTask.VIDEO_SCRIPT || platform==SocialPlatform.TIKTOK || platform==SocialPlatform.YOUTUBE);
    }

    @AssertTrue(message="Cần cung cấp bản nháp để tối ưu.")
    public boolean isOptimizeDraftPresent() {
        return (task!=AiContentTask.OPTIMIZE && task!=AiContentTask.PROOFREAD) ||
            (existingContent!=null && !existingContent.isBlank());
    }

}
