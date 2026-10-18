package com.example.fbservice.config;

import com.example.fbservice.model.User;
import org.springframework.security.core.authority.AuthorityUtils;

/** Authenticated database identity; distinguish it from test-only mock principals. */
public final class AccountPrincipal extends org.springframework.security.core.userdetails.User {
    private final Long accountId;
    private final long authVersion;

    public AccountPrincipal(User account, String role) {
        super(account.getUsername(), account.getPassword(), account.isEnabled(), true,
              true, true, AuthorityUtils.createAuthorityList("ROLE_" + role));
        this.accountId = account.getId();
        this.authVersion = account.getAuthVersion();
    }

    public Long getAccountId() {
        return accountId;
    }

    public long getAuthVersion() {
        return authVersion;
    }
}
