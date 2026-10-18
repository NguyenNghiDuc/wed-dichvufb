package com.example.fbservice;

import com.example.fbservice.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportCounts27;DB_CLOSE_DELAY=-1")
class SupportTicketCountsIntegrationTest {
    @Autowired SupportTicketService tickets;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="summary-customer-a")
    void customerCountersNeverIncludeAnotherCustomersTickets() throws Exception {
        var open = tickets.create("summary-customer-a", "Cần giải đáp A",
            "Yêu cầu chưa nhận được phản hồi từ nhân viên.");
        var answered = tickets.create("summary-customer-a", "Cần giải đáp B",
            "Yêu cầu đã được nhân viên trả lời nội dung.");
        var closed = tickets.create("summary-customer-a", "Cần giải đáp C",
            "Yêu cầu sẽ được đóng sau khi hoàn thành.");
        tickets.replyAsAdmin("summary-operator", answered.getId(), "Câu trả lời tư vấn");
        tickets.closeAsAdmin("summary-operator", closed.getId());
        tickets.create("summary-customer-b", "Riêng của B",
            "Yêu cầu này thuộc tài khoản khách hàng khác.");

        assertThat(tickets.countsForOwner("summary-customer-a"))
            .isEqualTo(new SupportTicketCounts(1, 1, 1));
        assertThat(tickets.countsForOwner("summary-customer-b"))
            .isEqualTo(new SupportTicketCounts(1, 0, 0));
        assertThat(tickets.countsForStaff())
            .isEqualTo(new SupportTicketCounts(2, 1, 1));

        mvc.perform(get("/support"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("ticketCounts",
                new SupportTicketCounts(1, 1, 1)));
        mvc.perform(get("/support").with(user("summary-customer-b")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("ticketCounts",
                new SupportTicketCounts(1, 0, 0)));
        mvc.perform(get("/admin/support")
            .with(user("summary-active-staff").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("ticketCounts",
                new SupportTicketCounts(2, 1, 1)));
        mvc.perform(get("/support/stats"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.open").value(1))
            .andExpect(jsonPath("$.answered").value(1))
            .andExpect(jsonPath("$.closed").value(1));
        mvc.perform(get("/support/stats").with(user("summary-customer-b")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.open").value(1))
            .andExpect(jsonPath("$.answered").value(0));
        mvc.perform(get("/admin/support/stats")
                .with(user("summary-active-staff").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.open").value(2))
            .andExpect(jsonPath("$.answered").value(1))
            .andExpect(jsonPath("$.closed").value(1));
        mvc.perform(get("/admin/support/stats"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/support"))
            .andExpect(status().isForbidden());
        assertThat(open.getStatus()).isEqualTo(TicketStatus.OPEN);
    }
}
