package com.example.fbservice;

import com.example.fbservice.support.SupportTicketRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W9 integration coverage for bounded duplicate/high-rate support requests.
 * Accounts are generated per test to avoid leaking counters across test contexts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:w9supportratelimit;DB_CLOSE_DELAY=-1")
class W9SupportRateLimitIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketRepository tickets;

    private String uniqueOwner() {
        return "w9_limit_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }

    private long count(String owner) {
        return tickets.findByOwnerUsername(owner, PageRequest.of(0, 10)).getTotalElements();
    }

    @Test
    void repeatedRequestsAreLimitedAndAnotherAccountRetainsItsOwnQuota() throws Exception {
        String owner = uniqueOwner();
        String separateOwner = uniqueOwner();
        for (int index = 0; index < 5; index++) {
            mvc.perform(post("/support").with(user(owner)).with(csrf())
                    .param("subject", "W9 limited request " + index)
                    .param("description", "Repeated test ticket within a per-user quota."))
                .andExpect(status().is3xxRedirection());
        }
        assertThat(count(owner)).isEqualTo(5);

        mvc.perform(post("/support").with(user(owner)).with(csrf())
                .param("subject", "Blocked request")
                .param("description", "This request must not create another ticket."))
            .andExpect(status().isTooManyRequests());
        assertThat(count(owner)).as("Rate-limited request must not be persisted").isEqualTo(5);

        mvc.perform(post("/support").with(user(separateOwner)).with(csrf())
                .param("subject", "Independent user request")
                .param("description", "A second user should have an independent rate quota."))
            .andExpect(status().is3xxRedirection());
        assertThat(count(separateOwner)).isEqualTo(1);
    }

    @Test
    void invalidInputAndMissingCsrfCannotCreateSupportTicket() throws Exception {
        String owner = uniqueOwner();
        mvc.perform(post("/support").with(user(owner)).with(csrf())
                .param("subject", "bad")
                .param("description", "short"))
            .andExpect(status().isOk())
            .andExpect(view().name("support_form"));
        mvc.perform(post("/support").with(user(owner))
                .param("subject", "Missing anti-forgery token")
                .param("description", "This write must be rejected without csrf."))
            .andExpect(status().isForbidden());
        assertThat(count(owner)).isZero();
    }
}
