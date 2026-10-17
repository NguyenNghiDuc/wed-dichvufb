package com.example.fbservice.config;

import com.example.fbservice.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Revokes sessions after account removal, suspension, or role changes.
 * Public login and error pages must remain accessible even with an old session.
 */
public final class AccountStatusFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
        "/", "/login", "/register", "/help", "/error",
        "/favicon.ico", "/favicon.svg", "/style.css",
        "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"
    );
    private final UserRepository users;

    public AccountStatusFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request.getDispatcherType() == DispatcherType.ERROR) return true;
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) return false;
        // Servlet path can be empty under servlet forwarding/MockMvc; use the actual request URI.
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        return PUBLIC_GET_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            boolean valid = users.findById(principal.getAccountId()).map(account ->
                account.isEnabled()
                && account.getUsername().equals(principal.getUsername())
                // Authentication providers erase UserDetails credentials after login.
                // A separate digest allows password rotation to revoke existing sessions.
                && principal.isCredentialCurrent(account.getPassword())
                // A database account has exactly one role. Do not accept a session
                // with extra authorities that might accidentally preserve privileges.
                && authentication.getAuthorities().size() == 1
                && authentication.getAuthorities().stream()
                    .allMatch(authority -> authority.getAuthority().equals("ROLE_" + account.getRole()))
            ).orElse(false);
            if (!valid) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                response.setHeader("Cache-Control", "no-store");
                if ("GET".equalsIgnoreCase(request.getMethod())
                        || "HEAD".equalsIgnoreCase(request.getMethod())) {
                    // An anonymous filter has already run earlier in the security chain.
                    // Explicitly redirect to avoid an authorization fallback 403.
                    response.setStatus(HttpServletResponse.SC_FOUND);
                    response.setHeader("Location", request.getContextPath() + "/login?sessionExpired");
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
