package com.example.fbservice;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:errorloginregression;DB_CLOSE_DELAY=-1")
class ErrorLoginRedirectRegressionTest {
    @Autowired MockMvc mvc;

    @Test
    void anonymousErrorDispatchIsNotRedirectedToLogin() throws Exception {
        mvc.perform(get("/error?continue").with(request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/missing-page");
            return request;
        }))
        .andExpect(status().isNotFound());
    }

    @Test
    void staticFaviconDoesNotCreateAnAuthenticationRedirect() throws Exception {
        mvc.perform(get("/favicon.ico"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/favicon.svg"));
        mvc.perform(get("/favicon.svg"))
            .andExpect(status().isOk());
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/favicon.svg")));
    }

    @Test
    void unsuccessfulPrivatePageNavigationDoesNotOverridePostLoginLandingPage() throws Exception {
        var response = mvc.perform(get("/campaigns"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) response.getRequest().getSession(false);
        org.assertj.core.api.Assertions.assertThat(session).isNotNull();

        mvc.perform(get("/error?continue").session(session).with(request -> {
            request.setDispatcherType(DispatcherType.ERROR);
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
            return request;
        }))
        .andExpect(status().isNotFound());

        mvc.perform(get("/admin/campaign-orders").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "ordinary-user")
    void regularUserCannotEnterAdmin() throws Exception {
        mvc.perform(get("/admin/campaign-orders")).andExpect(status().isForbidden());
    }
}
