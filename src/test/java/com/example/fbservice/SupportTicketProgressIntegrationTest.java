package com.example.fbservice;

import com.example.fbservice.support.SupportTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:supportProgressW3;DB_CLOSE_DELAY=-1")
class SupportTicketProgressIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;

    @Test
    @WithMockUser(username = "progress-owner")
    void ownerAndStaffSeeProgressButUnrelatedCustomerSeesNothing() throws Exception {
        var mine = tickets.create("progress-owner", "Theo dõi xử lý ticket",
                "Tôi muốn biết ngày tạo và cập nhật gần đây của yêu cầu.");
        var foreign = tickets.create("progress-other-owner", "Ticket hoàn toàn riêng tư",
                "Khách hàng khác không được truy cập thông tin nội bộ.");
        String url = "/support/" + mine.getId() + "/progress";
        mvc.perform(get(url))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.priority").value("NORMAL"))
            .andExpect(jsonPath("$.assigned").value(false))
            .andExpect(jsonPath("$.ageMinutes").isNumber())
            .andExpect(jsonPath("$.idleMinutes").isNumber())
            .andExpect(content().string(not(containsString("progress-other-owner"))));
        var result = tickets.progressForOwner("progress-owner", mine.getId());
        assertThat(result.ageMinutes()).isGreaterThanOrEqualTo(0L);
        assertThat(result.idleMinutes()).isGreaterThanOrEqualTo(0L);
        mvc.perform(get("/support/" + mine.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tiến độ xử lý")))
            .andExpect(content().string(containsString("Thời gian từ lúc tạo")));
        mvc.perform(get("/support/" + foreign.getId() + "/progress"))
            .andExpect(status().isNotFound());
        mvc.perform(get(url).with(user("progress-other-owner")))
            .andExpect(status().isNotFound());
        mvc.perform(get("/admin/support/" + mine.getId() + "/progress"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/support/" + mine.getId() + "/progress")
                    .with(user("progress-staff").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(get("/admin/support/" + mine.getId())
                    .with(user("progress-staff").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tiến độ xử lý")));
    }

    @Test
    void anonymousCannotReadProgressEndpoints() throws Exception {
        mvc.perform(get("/support/1/progress"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/support/1/progress"))
            .andExpect(status().is3xxRedirection());
    }
}
