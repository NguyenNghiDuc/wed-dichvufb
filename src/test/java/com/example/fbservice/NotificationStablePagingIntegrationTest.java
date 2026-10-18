package com.example.fbservice;

import com.example.fbservice.notification.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:notificationstablepaging;DB_CLOSE_DELAY=-1")
class NotificationStablePagingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AccountNotificationRepository notifications;
    @Autowired JdbcTemplate jdbc;

    @SuppressWarnings("unchecked")
    private List<AccountNotification> page(int index) throws Exception {
        var result=mvc.perform(get("/notifications").param("page",String.valueOf(index)))
            .andExpect(status().isOk()).andReturn();
        return (List<AccountNotification>) result.getModelAndView().getModel().get("items");
    }

    @Test
    @WithMockUser(username="stable-order-owner")
    void timestampTiesUseIdDescendingWithoutDuplicatingRowsAcrossPages() throws Exception {
        var records=new ArrayList<AccountNotification>();
        for(int i=0;i<25;i++) {
            records.add(new AccountNotification("stable-order-owner","Thông báo #"+i,"Mô tả "+i));
        }
        records= new ArrayList<>(notifications.saveAllAndFlush(records));
        jdbc.update("UPDATE account_notifications SET created_at = ? WHERE owner_username = ?",
            Timestamp.valueOf(LocalDateTime.of(2026,10,18,9,0,0)),"stable-order-owner");

        var first=page(0);
        var second=page(1);
        assertThat(first).hasSize(20);
        assertThat(second).hasSize(5);

        var allIds=new ArrayList<Long>();
        first.forEach(n->allIds.add(n.getId()));
        second.forEach(n->allIds.add(n.getId()));
        assertThat(allIds).doesNotHaveDuplicates();
        assertThat(allIds).isSortedAccordingTo(java.util.Comparator.reverseOrder());

        var expected=records.stream().map(AccountNotification::getId)
            .sorted(java.util.Comparator.reverseOrder()).toList();
        assertThat(allIds).containsExactlyElementsOf(expected);
    }
}
