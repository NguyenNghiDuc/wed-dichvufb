package com.example.fbservice;

import com.example.fbservice.notification.AccountNotification;
import com.example.fbservice.notification.AccountNotificationRepository;
import org.junit.jupiter.api.Test;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:notificationInbox;DB_CLOSE_DELAY=-1")
class NotificationInboxIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AccountNotificationRepository repo;
    @Autowired JdbcTemplate jdbc;

    private void seed(String owner, int total) {
        for (int i = 0; i < total; i++) {
            repo.save(new AccountNotification(owner, "Thông báo " + i, "Thông tin thử nghiệm " + i));
        }
    }

    @Test
    @WithMockUser(username="inbox-paging-owner")
    void inboxPaginatesWithOwnerScopeAndUnreadFilter() throws Exception {
        seed("inbox-paging-owner", 23);
        seed("inbox-paging-other", 2);
        mvc.perform(get("/notifications"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(model().attribute("items", hasSize(20)))
            .andExpect(model().attribute("unread", 23L))
            .andExpect(model().attribute("hasNext", true));
        mvc.perform(get("/notifications").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("items", hasSize(3)))
            .andExpect(model().attribute("hasPrevious", true));
        mvc.perform(get("/notifications").param("unreadOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("items", hasSize(20)))
            .andExpect(model().attribute("unreadOnly", true));
        mvc.perform(get("/notifications").with(user("inbox-paging-outsider")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("items", hasSize(0)))
            .andExpect(model().attribute("unread", 0L));
    }

    @Test
    @WithMockUser(username="inbox-bulk-owner")
    void markAllRequiresCsrfIsIdempotentAndNeverChangesOtherCustomer() throws Exception {
        seed("inbox-bulk-owner", 3);
        seed("inbox-bulk-other", 2);
        var first = repo.findTop100ByOwnerUsernameOrderByCreatedAtDesc("inbox-bulk-owner").get(0);
        mvc.perform(post("/notifications/read-all"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/notifications/" + first.getId() + "/read").with(csrf())
                .with(user("inbox-bulk-other")))
            .andExpect(status().isNotFound());
        mvc.perform(post("/notifications/read-all").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(repo.countByOwnerUsernameAndReadAtIsNull("inbox-bulk-owner")).isZero();
        assertThat(repo.countByOwnerUsernameAndReadAtIsNull("inbox-bulk-other")).isEqualTo(2);
        var firstReadTime = repo.findById(first.getId()).orElseThrow().getReadAt();
        mvc.perform(post("/notifications/read-all").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(repo.findById(first.getId()).orElseThrow().getReadAt()).isEqualTo(firstReadTime);
        mvc.perform(get("/notifications").param("unreadOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("items", hasSize(0)));
    }

    @Test
    @WithMockUser(username="inbox-validation-owner")
    void rejectsInvalidPageWithoutUnboundedDatabaseQuery() throws Exception {
        mvc.perform(get("/notifications").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/notifications").param("page", "10001")).andExpect(status().isBadRequest());
        mvc.perform(get("/notifications").param("page", "not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/notifications").param("unreadOnly", "nonsense"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username="inbox-date-owner")
    void dateFilterKeepsOwnerAndUnreadIsolation() throws Exception {
        seed("inbox-date-owner", 3);
        seed("inbox-date-other", 2);
        var oldest = repo.findTop100ByOwnerUsernameOrderByCreatedAtDesc("inbox-date-owner").get(0);
        jdbc.update("UPDATE account_notifications SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDate.now().minusDays(45).atStartOfDay()), oldest.getId());
        mvc.perform(get("/notifications").param("days", "7"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("items", hasSize(2)))
            .andExpect(model().attribute("days", 7))
            .andExpect(model().attribute("unread", 3L));
        mvc.perform(get("/notifications").param("days", "30").param("unreadOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("items", hasSize(2)))
            .andExpect(model().attribute("unreadOnly", true));
        mvc.perform(get("/notifications").param("days", "90"))
            .andExpect(model().attribute("items", hasSize(3)));
        mvc.perform(get("/notifications").param("days", "7").with(user("inbox-date-other")))
            .andExpect(model().attribute("items", hasSize(2)))
            .andExpect(model().attribute("unread", 2L));
    }

    @Test
    @WithMockUser(username="inbox-period-validation")
    void invalidPeriodIsRejectedInsteadOfQueryingUnboundedHistory() throws Exception {
        for (String days : new String[]{"-1", "1", "365", "bad"}) {
            mvc.perform(get("/notifications").param("days", days))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void anonymousCannotReadOrUpdateNotifications() throws Exception {
        mvc.perform(get("/notifications")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/notifications/read-all").with(csrf()))
            .andExpect(status().is3xxRedirection());
    }
}
