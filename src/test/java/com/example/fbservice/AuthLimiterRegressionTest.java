package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The throttler must not allow CSRF-less POSTs to lock out accounts. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:authlimiterregression;DB_CLOSE_DELAY=-1")
class AuthLimiterRegressionTest {
    @Autowired MockMvc mvc;

    @Test
    void missingCsrfDoesNotConsumeLoginAllowance() throws Exception {
        for (int i=0; i<12; i++) {
            mvc.perform(post("/login").with(req -> {
                    req.setRemoteAddr("198.51.100.92");
                    return req;
                }).param("username","csrf-free-replay").param("password","invalid"))
                .andExpect(status().isForbidden());
        }
        mvc.perform(post("/login").with(csrf()).with(req -> {
                req.setRemoteAddr("198.51.100.92");
                return req;
            }).param("username","csrf-free-replay").param("password","invalid"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void oneAccountDoesNotBlockAnotherBehindSameAddress() throws Exception {
        for (int i=0; i<10; i++) {
            mvc.perform(post("/login").with(csrf()).with(req -> {
                    req.setRemoteAddr("198.51.100.93");
                    req.addHeader("X-Forwarded-For", "203.0.113." + i);
                    return req;
                }).param("username","rejected-account").param("password","wrong"))
                .andExpect(status().is3xxRedirection());
        }
        mvc.perform(post("/login").with(csrf()).with(req -> {
                req.setRemoteAddr("198.51.100.93");
                req.addHeader("X-Forwarded-For", "203.0.113.250");
                return req;
            }).param("username","rejected-account").param("password","wrong"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After","600"));
        mvc.perform(post("/login").with(csrf()).with(req -> {
                req.setRemoteAddr("198.51.100.93");
                return req;
            }).param("username","unrelated-account").param("password","wrong"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void registerAlsoRequiresCsrfBeforeConsumingRateAllowance() throws Exception {
        for (int i=0; i<11; i++) {
            mvc.perform(post("/register").with(req -> {
                    req.setRemoteAddr("198.51.100.94");
                    return req;
                }).param("username","missing-csrf-"+i).param("password","strong-throwaway-password"))
                .andExpect(status().isForbidden());
        }
        mvc.perform(post("/register").with(csrf()).with(req -> {
                req.setRemoteAddr("198.51.100.94");
                return req;
            }).param("username","registered-after-csrf").param("password","strong-throwaway-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
    }
}
