package com.example.fbservice.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;

/** Reject stale authentication POSTs, then offer a fresh form without replaying credentials. */
public final class AuthenticationAccessDeniedHandler implements AccessDeniedHandler {
    private final AccessDeniedHandler fallback = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException, ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (exception instanceof CsrfException && "POST".equals(request.getMethod())
                && ("/login".equals(path) || "/register".equals(path))) {
            // A relative redirect preserves the browser's HTTPS origin through a development proxy.
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
            response.setHeader("Location", request.getContextPath() + path + "?sessionExpired");
            response.setHeader("Cache-Control", "no-store");
            return;
        }
        fallback.handle(request, response, exception);
    }
}
