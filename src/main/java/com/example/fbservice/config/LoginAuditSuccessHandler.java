package com.example.fbservice.config;

import com.example.fbservice.security.SecurityActivity;
import com.example.fbservice.security.SecurityActivityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** Called only for successful username/password authentication. */
@Component
public class LoginAuditSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final SecurityActivityService activity;
    private final LoginFailureTracker failures;

    public LoginAuditSuccessHandler(SecurityActivityService activity, LoginFailureTracker failures) {
        this.activity=activity;
        this.failures=failures;
        setDefaultTargetUrl("/campaigns");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof AccountPrincipal account)) {
            throw new ServletException("Database account principal required");
        }
        try {
            // Authentication context is already saved by Spring Security when this handler runs.
            // If the required audit write fails, invalidate it explicitly rather than
            // returning a 500 while an unrecorded authenticated session remains usable.
            activity.record(account.getAccountId(), SecurityActivity.Kind.SIGN_IN);
        } catch (RuntimeException unavailable) {
            SecurityContextHolder.clearContext();
            HttpSession session=request.getSession(false);
            if(session!=null)session.invalidate();
            response.setHeader("Cache-Control","no-store");
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        failures.resetAfterSuccess(account.getUsername());
        super.onAuthenticationSuccess(request,response,authentication);
    }
}
