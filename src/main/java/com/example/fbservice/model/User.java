package com.example.fbservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 255)
    private String email;

    @Column(name = "display_name", length = 80)
    private String displayName;

    @Column(name = "auth_version", nullable = false, columnDefinition = "bigint default 0")
    private long authVersion;

    @Column(name = "failed_login_count", nullable = false, columnDefinition = "integer default 0")
    private int failedLoginCount;

    @Column(name = "failed_login_window")
    private java.time.LocalDateTime failedLoginWindow;

    @Column(name = "login_locked_until")
    private java.time.LocalDateTime loginLockedUntil;

    @Column(nullable = false, length = 16)
    private String role = "USER";

    @Column(nullable = false)
    private boolean enabled = true;
}
