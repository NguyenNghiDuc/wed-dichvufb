package com.example.fbservice;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Cross-module browser-session journey: registration -> real form login -> campaign
 * -> tracking -> support. Never uses @WithMockUser or a forged principal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w9customerjourney;DB_CLOSE_DELAY=-1")
class W9CustomerJourneyIntegrationTest {
    private static final String PASSWORD = "W9-test-password-123!";
    private static final String VALID_CAMPAIGN = """
        {"platform":"YOUTUBE","serviceType":"PROFILE_AUDIT",
         "targetUrl":"https://www.youtube.com/@growlab-demo",
         "brief":"Kiểm tra hồ sơ và lập kế hoạch nội dung cho kênh học tập."}
        """;

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CampaignOrderRepository orders;
    @Autowired SupportTicketRepository tickets;

    private String newUsername() {
        return "w9_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private MockHttpSession registerAndLogin(String username) throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("username", username).param("password", PASSWORD)
                .param("email", username + "@example.test"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
        var persisted = users.findByUsername(username).orElseThrow();
        assertThat(persisted.getPassword()).startsWith("$2").doesNotContain(PASSWORD);
        assertThat(persisted.getRole()).isEqualTo("USER");

        MvcResult login = mvc.perform(post("/login").with(csrf())
                .param("username", username).param("password", PASSWORD))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/campaigns"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).as("Successful login must create a persistent HTTP session").isNotNull();
        return session;
    }

    @Test
    void browserJourneyTracksOnlyOwnOrderAndTicket() throws Exception {
        String owner = newUsername();
        String outsider = newUsername();
        MockHttpSession ownerSession = registerAndLogin(owner);
        MockHttpSession outsiderSession = registerAndLogin(outsider);

        MvcResult created = mvc.perform(post("/api/v1/campaign-orders")
                .session(ownerSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CAMPAIGN))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerUsername").value(owner))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
            .andReturn();
        String orderLocation = created.getResponse().getHeader("Location");
        assertThat(orderLocation).startsWith("/api/v1/campaign-orders/");
        long orderId = Long.parseLong(orderLocation.substring(orderLocation.lastIndexOf('/') + 1));

        mvc.perform(get("/campaigns").session(ownerSession))
            .andExpect(status().isOk());
        mvc.perform(get("/campaigns/" + orderId).session(ownerSession))
            .andExpect(status().isOk());
        mvc.perform(get(orderLocation + "/history").session(ownerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(orderLocation).session(outsiderSession))
            .andExpect(status().isNotFound());
        mvc.perform(get("/campaigns/" + orderId).session(outsiderSession))
            .andExpect(status().isNotFound());
        mvc.perform(get(orderLocation + "/history").session(outsiderSession))
            .andExpect(status().isNotFound());

        String subject = "Support for GrowLab campaign " + orderId;
        MvcResult createdTicket = mvc.perform(post("/support").session(ownerSession).with(csrf())
                .param("subject", subject)
                .param("description", "Xin kiểm tra trạng thái của chiến dịch mới tạo."))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        String ticketLocation = createdTicket.getResponse().getRedirectedUrl();
        assertThat(ticketLocation).startsWith("/support/");
        long ticketId = Long.parseLong(ticketLocation.substring(ticketLocation.lastIndexOf('/') + 1));
        mvc.perform(get(ticketLocation).session(ownerSession))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(subject)));
        mvc.perform(get(ticketLocation).session(outsiderSession))
            .andExpect(status().isNotFound());
        mvc.perform(post(ticketLocation + "/reply").session(outsiderSession).with(csrf())
                .param("message", "This should never be written."))
            .andExpect(status().isNotFound());

        assertThat(orders.findByIdAndOwnerUsername(orderId, owner)).isPresent();
        assertThat(orders.findByIdAndOwnerUsername(orderId, outsider)).isEmpty();
        assertThat(tickets.findByIdAndOwnerUsername(ticketId, owner)).isPresent();
        assertThat(tickets.findByIdAndOwnerUsername(ticketId, outsider)).isEmpty();
    }

    @Test
    void boundariesRejectMissingCsrfInvalidInputAndUnauthorizedActions() throws Exception {
        String username = newUsername();
        mvc.perform(post("/register").param("username", username)
                .param("password", PASSWORD))
            .andExpect(status().isForbidden());
        assertThat(users.findByUsername(username)).isEmpty();

        mvc.perform(post("/register").with(csrf()).param("username", "<script>")
                .param("password", PASSWORD))
            .andExpect(status().isOk())
            .andExpect(view().name("register"));
        assertThat(users.findByUsername("<script>")).isEmpty();

        mvc.perform(get("/campaigns")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/support")).andExpect(status().is3xxRedirection());
        MockHttpSession session = registerAndLogin(username);

        mvc.perform(post("/api/v1/campaign-orders").session(session)
                .contentType(MediaType.APPLICATION_JSON).content(VALID_CAMPAIGN))
            .andExpect(status().isForbidden());
        mvc.perform(post("/support").session(session)
                .param("subject", "Cannot post")
                .param("description", "Missing anti-forgery token."))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/campaign-orders").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"platform":"YOUTUBE","serviceType":"PROFILE_AUDIT",
                     "targetUrl":"https://example.invalid/other",
                     "brief":"This is an intentionally invalid platform link."}
                    """))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/campaign-orders").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/support").session(session).with(csrf())
                .param("subject", "x").param("description", "too short"))
            .andExpect(status().isOk())
            .andExpect(view().name("support_form"));
        mvc.perform(get("/admin/campaign-orders").session(session))
            .andExpect(status().isForbidden());

        assertThat(orders.findByOwnerUsername(username,
                org.springframework.data.domain.PageRequest.of(0, 5)).getTotalElements()).isZero();
        assertThat(tickets.findByOwnerUsername(username,
                org.springframework.data.domain.PageRequest.of(0, 5)).getTotalElements()).isZero();
    }
}
