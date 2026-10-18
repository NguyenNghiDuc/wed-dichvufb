package com.example.fbservice.content;

import com.example.fbservice.campaign.SocialPlatform;
import jakarta.validation.Valid;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/** Data is provided voluntarily by the signed-in user, NOT verified by any social provider. */
public record ContentMetricsRequest(
    @NotNull @Size(min=1,max=100) List<@NotNull @Valid Metric> posts) {

    public record Metric(
        @NotNull SocialPlatform platform,
        @NotNull LocalDateTime publishedAtUtc,
        @Min(1) @Max(1_000_000_000L) @JsonDeserialize(using=StrictMetricCountDeserializer.class) long views,
        @Min(0) @Max(1_000_000_000L) @JsonDeserialize(using=StrictMetricCountDeserializer.class) long likes,
        @Min(0) @Max(1_000_000_000L) @JsonDeserialize(using=StrictMetricCountDeserializer.class) long comments,
        @Min(0) @Max(1_000_000_000L) @JsonDeserialize(using=StrictMetricCountDeserializer.class) long shares,
        @Min(0) @Max(1_000_000_000L) @JsonDeserialize(using=StrictMetricCountDeserializer.class) long saves) {}
}
