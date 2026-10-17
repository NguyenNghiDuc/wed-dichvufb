package com.example.fbservice.config;

import com.example.fbservice.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revalidate persisted status and role for each authenticated session request. */
public final class AccountStatusFilter extends OncePerRequestFilter {
    private final UserRepository users;

    public AccountStatusFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            boolean valid = users.findById(principal.getAccountId()).map(account ->
                account.isEnabled()
                && account.getUsername().equals(principal.getUsername())
                && authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + account.getRole()))
            ).orElse(false);
            if (!valid) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                // Treat the revoked session as anonymous, not as a forbidden request.
                // This must also work when the browser lands on public /login with a stale cookie.
                // AuthorizationFilter will redirect protected requests to the login page.
                chain.doFilter(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
