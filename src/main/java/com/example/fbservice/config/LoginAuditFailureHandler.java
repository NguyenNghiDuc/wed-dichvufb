package com.example.fbservice.config;

import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.SecurityActivity;
import com.example.fbservice.security.SecurityActivityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Account-scoped security activity on denied form login.
 * No username, password, IP, token, user agent or session data is persisted.
 * Requests without CSRF and throttled requests never reach this handler.
 */
@Component
public class LoginAuditFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final UserRepository users;
    private final SecurityActivityService securityEvents;

    public LoginAuditFailureHandler(UserRepository users, SecurityActivityService securityEvents) {
        super("/login?error");
        this.users = users;
        this.securityEvents = securityEvents;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        String submitted = request.getParameter("username");
        // Only record an event for a real, syntactically valid account.
        // This does not disclose whether the account exists to the caller.
        if (submitted != null && submitted.matches("[A-Za-z0-9._-]{3,40}")) {
            users.findByUsername(submitted).ifPresent(account ->
                securityEvents.record(account.getId(), SecurityActivity.Kind.SIGN_IN_FAILED));
        }
        response.setHeader("Cache-Control", "no-store");
        super.onAuthenticationFailure(request, response, exception);
    }
}
