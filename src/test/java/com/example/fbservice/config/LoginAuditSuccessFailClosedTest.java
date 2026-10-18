package com.example.fbservice.config;

import com.example.fbservice.model.User;
import com.example.fbservice.security.SecurityActivity;
import com.example.fbservice.security.SecurityActivityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginAuditSuccessFailClosedTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private UsernamePasswordAuthenticationToken account() {
        User user = new User();
        user.setId(73421L);
        user.setUsername("audit-write-owner");
        user.setRole("USER");
        user.setPassword("test-encoded-password-not-a-real-credential");
        AccountPrincipal principal=new AccountPrincipal(user,"USER");
        return new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
    }

    @Test
    void failedSecurityAuditDoesNotLeaveAnAuthenticatedSession() throws Exception {
        var activity=mock(SecurityActivityService.class);
        doThrow(new IllegalStateException("test-only database outage"))
            .when(activity).record(73421L,SecurityActivity.Kind.SIGN_IN);
        var handler=new LoginAuditSuccessHandler(activity,new LoginFailureTracker());
        var req=new MockHttpServletRequest("POST","/login");
        MockHttpSession session=(MockHttpSession)req.getSession(true);
        var resp=new MockHttpServletResponse();
        var auth=account();
        SecurityContextHolder.getContext().setAuthentication(auth);

        handler.onAuthenticationSuccess(req,resp,auth);
        assertThat(resp.getStatus()).isEqualTo(503);
        assertThat(resp.getHeader("Cache-Control")).contains("no-store");
        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(resp.getRedirectedUrl()).isNull();
    }

    @Test
    void successfulAuditStillUsesSafeFixedRedirect() throws Exception {
        var activity=mock(SecurityActivityService.class);
        var handler=new LoginAuditSuccessHandler(activity,new LoginFailureTracker());
        var req=new MockHttpServletRequest("POST","/login");
        req.addParameter("next","https://phishing.invalid/logout");
        var resp=new MockHttpServletResponse();
        handler.onAuthenticationSuccess(req,resp,account());
        verify(activity).record(73421L,SecurityActivity.Kind.SIGN_IN);
        assertThat(resp.getRedirectedUrl()).isEqualTo("/campaigns");
    }
}
