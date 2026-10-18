package com.example.fbservice.admin;

/** Aggregated account counts, no profile information or credential material. */
public record AdminUserRoleCount(String role, long total, long enabled) {
    public long locked() { return total - enabled; }
}
