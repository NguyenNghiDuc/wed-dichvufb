package com.example.fbservice.config;

import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.AccountSessionService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revalidate persisted status and role for each authenticated session request. */
public final class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository users;

    private final AccountSessionService sessions;

    public AccountStatusFilter(UserRepository users,AccountSessionService sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            boolean valid = users.findById(principal.getAccountId()).map(account ->
                account.isEnabled()
                && account.getAuthVersion() == principal.getAuthVersion()
                && account.getUsername().equals(principal.getUsername())
                && authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + account.getRole()))
            ).orElse(false);
            if (!valid || !sessions.validateAndTouch(principal,request.getSession(false))) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account unavailable");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
