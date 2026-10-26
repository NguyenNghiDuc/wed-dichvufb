package com.example.fbservice.config;

import jakarta.persistence.*;

/** Stores only a SHA-256 bucket identity, never raw usernames or network addresses. */
@Entity @Table(name="rate_limit_windows",indexes=@Index(name="idx_rate_limit_expiry",columnList="expires_at_ms"))
public class RateLimitWindow {
 @Id @Column(name="bucket_hash",length=64) private String bucketHash;
 @Column(name="expires_at_ms",nullable=false) private long expiresAtMillis;
 @Column(name="hits",nullable=false) private int hits;
 protected RateLimitWindow(){}
}
