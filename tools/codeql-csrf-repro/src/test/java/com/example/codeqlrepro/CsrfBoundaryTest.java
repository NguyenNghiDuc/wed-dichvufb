package com.example.codeqlrepro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CsrfBoundaryTest {
    @Autowired
    MockMvc mvc;

    @Test
    void readOnlyGetHandlersRemainReadable() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string("login"));
        mvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string("register"));
        mvc.perform(get("/edit"))
            .andExpect(status().isOk())
            .andExpect(content().string("edit"));
        mvc.perform(get("/view-login"))
            .andExpect(status().isOk())
            .andExpect(content().string("login"));
    }

    @Test
    void diagnosticSelfEdgeControlRemainsReadOnlyAndDoesNotRecurse() throws Exception {
        mvc.perform(get("/edit-self-edge"))
            .andExpect(status().isOk())
            .andExpect(content().string("edit-self-edge"));
    }

    @Test
    void unsafePostRejectsMissingCsrfAndAcceptsValidTestToken() throws Exception {
        mvc.perform(post("/register"))
            .andExpect(status().isForbidden());

        mvc.perform(post("/register").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string("registered"));
    }
}
