package com.example.fbservice.config;

import com.example.fbservice.security.SecurityActivity;
import com.example.fbservice.security.SecurityActivityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** Called only for successful username/password authentication. */
@Component
public class LoginAuditSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final SecurityActivityService activity;

    public LoginAuditSuccessHandler(SecurityActivityService activity) {
        this.activity=activity;
        setDefaultTargetUrl("/campaigns");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof AccountPrincipal account)) {
            throw new ServletException("Database account principal required");
        }
        activity.record(account.getAccountId(), SecurityActivity.Kind.SIGN_IN);
        super.onAuthenticationSuccess(request,response,authentication);
    }
}
