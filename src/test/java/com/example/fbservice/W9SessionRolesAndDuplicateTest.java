package com.example.fbservice;

import com.example.fbservice.campaign.CampaignEventRepository;
import com.example.fbservice.campaign.CampaignOrderRepository;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.support.SupportTicketRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** W9-only HTTP regression tests across authentication, roles, campaigns and support. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w9rolesduplicate;DB_CLOSE_DELAY=-1")
class W9SessionRolesAndDuplicateTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CampaignOrderRepository orders;
    @Autowired CampaignEventRepository events;
    @Autowired SupportTicketRepository tickets;

    private String uniqueUser() {
        return "w9_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private MockHttpSession authenticatedSession(String name) throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("username", name).param("password", "Local-W9-test-password-2026")
                .param("email", name + "@example.test"))
            .andExpect(status().is3xxRedirection());
        MvcResult result = mvc.perform(post("/login").with(csrf())
                .param("username", name)
                .param("password", "Local-W9-test-password-2026"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    @Test
    void logoutRequiresCsrfAndInvalidatesAuthenticatedSession() throws Exception {
        String name = uniqueUser();
        MockHttpSession session = authenticatedSession(name);
        mvc.perform(get("/campaigns").session(session))
            .andExpect(status().isOk());
        mvc.perform(post("/logout").session(session))
            .andExpect(status().isForbidden());
        assertThat(session.isInvalid()).isFalse();

        mvc.perform(post("/logout").session(session).with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/campaigns"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void roleBoundariesForUserStaffAndAdmin() throws Exception {
        mvc.perform(get("/admin/campaign-orders").with(user("w9_customer").roles("USER")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/support").with(user("w9_customer").roles("USER")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/staff/campaign-orders").with(user("w9_customer").roles("USER")))
            .andExpect(status().isForbidden());

        mvc.perform(get("/staff/campaign-orders").with(user("w9_staff").roles("STAFF")))
            .andExpect(status().isOk());
        mvc.perform(get("/admin/support").with(user("w9_staff").roles("STAFF")))
            .andExpect(status().isOk());
        mvc.perform(get("/admin/campaign-orders").with(user("w9_staff").roles("STAFF")))
            .andExpect(status().isForbidden());

        mvc.perform(get("/admin/campaign-orders").with(user("w9_admin").roles("ADMIN")))
            .andExpect(status().isOk());
        mvc.perform(get("/staff/campaign-orders").with(user("w9_admin").roles("ADMIN")))
            .andExpect(status().isOk());
        mvc.perform(get("/admin/support").with(user("w9_admin").roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void duplicateCancellationDoesNotCreateSecondAuditEntry() throws Exception {
        String name = uniqueUser();
        MockHttpSession session = authenticatedSession(name);
        String json = """
            {"platform":"YOUTUBE","serviceType":"PROFILE_AUDIT",
             "targetUrl":"https://www.youtube.com/@growlab-w9-duplicate",
             "brief":"Kiểm thử idempotency cho hành động hủy của người dùng."}
            """;
        String location = mvc.perform(post("/api/v1/campaign-orders").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        String cancel = location + "/cancel";

        mvc.perform(post(cancel).session(session))
            .andExpect(status().isForbidden());
        mvc.perform(post(cancel).session(session).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post(cancel).session(session).with(csrf()))
            .andExpect(status().isBadRequest());
        mvc.perform(get(location + "/history").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));
        assertThat(events.findByCampaignIdOrderByCreatedAtAsc(id)).hasSize(2);
    }

    @Test
    void submittedUntrustedHtmlIsRenderedAsTextAndApiInputFailuresDoNotWriteRows() throws Exception {
        String name = uniqueUser();
        MockHttpSession session = authenticatedSession(name);
        String malicious = "<script>alert('w9')</script>";
        long before = tickets.findByOwnerUsername(name,
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements();

        String location = mvc.perform(post("/support").session(session).with(csrf())
                .param("subject", malicious)
                .param("description", "This input must be displayed as text, never executed."))
            .andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith("/support/");
        mvc.perform(get(location).session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString(malicious))));
        assertThat(tickets.findByOwnerUsername(name,
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements())
            .isEqualTo(before + 1);

        long orderBefore = orders.findByOwnerUsername(name,
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements();
        mvc.perform(post("/api/v1/campaign-orders").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"platform\":"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/campaign-orders/page").session(session)
                .param("size", "1000"))
            .andExpect(status().isBadRequest());
        assertThat(orders.findByOwnerUsername(name,
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements())
            .isEqualTo(orderBefore);
    }
}
