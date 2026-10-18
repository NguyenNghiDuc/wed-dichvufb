package com.example.fbservice.security;

import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecurityActivityService {
    private final SecurityActivityRepository events;
    private final UserRepository users;

    public SecurityActivityService(SecurityActivityRepository events,UserRepository users) {
        this.events=events;this.users=users;
    }

    @Transactional
    public void record(Long accountId, SecurityActivity.Kind kind) {
        events.save(new SecurityActivity(accountId, kind));
    }

    /** SQL-side paging of the current account's events; never accepts a client account ID. */
    @Transactional(readOnly=true)
    public Page<SecurityActivity> page(AccountPrincipal principal, int page) {
        if (page < 0 || page > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid security history page");
        return events.findByAccountId(currentAccountId(principal),
            PageRequest.of(page, 20,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @Transactional(readOnly=true)
    public List<SecurityActivity> recent(AccountPrincipal principal) {
        return events.findTop20ByAccountIdOrderByCreatedAtDescIdDesc(currentAccountId(principal));
    }

    @Transactional(readOnly=true)
    public List<SecurityActivity> exportRecent(AccountPrincipal principal) {
        return events.findTop200ByAccountIdOrderByCreatedAtDescIdDesc(currentAccountId(principal));
    }

    private Long currentAccountId(AccountPrincipal principal) {
        if(principal==null || principal.getAccountId()==null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User account=users.findById(principal.getAccountId())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(!account.isEnabled() || !account.getUsername().equals(principal.getUsername())
                || !principal.isCredentialCurrent(account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return account.getId();
    }
}
