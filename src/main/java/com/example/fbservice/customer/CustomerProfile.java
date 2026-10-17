package com.example.fbservice.customer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Optional customer-managed contact details, separate from login/security identity. */
@Entity
@Table(name="customer_profiles")
public class CustomerProfile {
    @Id @Column(name="owner_username", nullable=false, length=100)
    private String ownerUsername;
    @Column(name="display_name", nullable=false, length=80)
    private String displayName = "";
    @Column(name="phone", nullable=false, length=20)
    private String phone = "";
    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected CustomerProfile() {}
    public CustomerProfile(String username) { this.ownerUsername = username; }

    public void update(String displayName, String phone) {
        this.displayName = displayName;
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }
    public String getOwnerUsername() { return ownerUsername; }
    public String getDisplayName() { return displayName; }
    public String getPhone() { return phone; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
