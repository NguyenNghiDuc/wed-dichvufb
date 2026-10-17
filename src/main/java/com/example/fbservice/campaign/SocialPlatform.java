package com.example.fbservice.campaign;

import java.util.Locale;

public enum SocialPlatform {
    FACEBOOK("Facebook", new String[]{"facebook.com", "fb.com"}),
    INSTAGRAM("Instagram", new String[]{"instagram.com"}),
    TIKTOK("TikTok", new String[]{"tiktok.com"}),
    YOUTUBE("YouTube", new String[]{"youtube.com", "youtu.be"});

    private final String displayName;
    private final String[] domains;

    SocialPlatform(String displayName, String[] domains) {
        this.displayName = displayName;
        this.domains = domains;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean acceptsHost(String host) {
        if (host == null) return false;
        String normalized = host.toLowerCase(Locale.ROOT);
        for (String domain : domains) {
            if (normalized.equals(domain) || normalized.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }
}
