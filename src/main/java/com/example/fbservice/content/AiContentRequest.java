package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import jakarta.validation.constraints.*;

public record AiContentRequest(@NotNull SocialPlatform platform,
                               @NotBlank @Size(min=3,max=150) String topic,
                               @NotBlank @Size(min=3,max=100) String audience) {}
