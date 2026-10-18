package com.example.fbservice.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CampaignOrderRequest(
    @NotNull SocialPlatform platform,
    @NotNull CampaignServiceType serviceType,
    @NotBlank @Size(max = 512) String targetUrl,
    @NotBlank @Size(min = 10, max = 1000) String brief,
    @Size(max = 32) String couponCode
) {
    public CampaignOrderRequest(SocialPlatform platform,CampaignServiceType serviceType,String targetUrl,String brief){this(platform,serviceType,targetUrl,brief,null);}
}
