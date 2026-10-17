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
}
