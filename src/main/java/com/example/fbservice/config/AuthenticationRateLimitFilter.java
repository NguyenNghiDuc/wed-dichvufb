package com.example.fbservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

/**
 * Defense in depth for login and registration. Runs INSIDE Spring Security,
 * after CSRF is checked, not as a global servlet filter. No trusted-proxy assumptions.
 * For multi-node production deploy a shared rate limiter at the gateway/Redis.
 */
@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private final TrafficLimiter limiter;

    public AuthenticationRateLimitFilter(TrafficLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        return !("/login".equals(path) || "/register".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String action = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        // The TCP peer, not the client-supplied X-Forwarded-For header.
        String peer = request.getRemoteAddr();
        if (peer == null || peer.isBlank()) peer = "unknown";
        String scope = "auth:" + action + ":" + peer;
        try {
            if ("/login".equals(action)) {
                // A shared NAT or Codespaces proxy should not lock unrelated accounts
                // after just 10 login attempts by another user.
                limiter.check(scope, 100, WINDOW);
                String username = request.getParameter("username");
                if (username == null || username.length() > 100) username = "(invalid)";
                limiter.check(scope + ":account:" + sha256(username), 10, WINDOW);
            } else {
                // Each completed registration attempt creates a durable DB write.
                limiter.check(scope, 10, WINDOW);
            }
        } catch (ResponseStatusException tooMany) {
            response.setHeader("Retry-After", "600");
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many authentication attempts");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Required SHA-256 algorithm unavailable", impossible);
        }
    }
}
