package com.example.fbservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

/** Shared limits per peer/account, plus a broader source budget against account rotation. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private final TrafficLimiter limiter;

    public AuthenticationRateLimitFilter(TrafficLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !("/login".equals(path) || "/register".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Use the TCP peer, never an untrusted X-Forwarded-For header supplied by the client.
        String action = request.getRequestURI().substring(request.getContextPath().length());
        String client = request.getRemoteAddr();
        try {
            String name=request.getParameter("username");
            name=name==null?"":name.strip();
            if(name.length()>100)name=name.substring(0,100);
            limiter.check("auth-source:"+action+":"+client,"/register".equals(action)?30:300,Duration.ofMinutes(10));
            limiter.check("auth:"+action+":"+client+":"+name,10,Duration.ofMinutes(10));
        } catch (ResponseStatusException exception) {
            response.setHeader("Retry-After", "600");
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many authentication attempts");
            return;
        }
        chain.doFilter(request, response);
    }
}
