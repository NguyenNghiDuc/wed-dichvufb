package com.example.fbservice;

import com.example.fbservice.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportMessageApi27;DB_CLOSE_DELAY=-1")
class SupportTicketMessagesApiIntegrationTest {
    @Autowired SupportTicketService tickets;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="api-ticket-owner")
    void customerHistoryIsBoundedPrivateAndReturnsMinimalJson() throws Exception {
        var ticket = tickets.create("api-ticket-owner", "Lịch sử hỗ trợ",
            "Khách hàng cần xem các phản hồi từ hệ thống.");
        var privateTicket = tickets.create("api-ticket-other", "Yêu cầu bảo mật",
            "Ticket thuộc một người dùng hoàn toàn khác.");
        for(int i=0;i<25;i++)
            tickets.replyAsOwner("api-ticket-owner",ticket.getId(),
                i==0?"<script>alert(1)</script>":"Phản hồi thứ "+i);
        String route="/support/"+ticket.getId()+"/messages";

        mvc.perform(get(route))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.totalElements").value(25))
            .andExpect(jsonPath("$.content",hasSize(20)))
            .andExpect(jsonPath("$.content[0].body").value("<script>alert(1)</script>"))
            .andExpect(jsonPath("$.content[0].author").value("api-ticket-owner"))
            .andExpect(jsonPath("$.content[0].ticketId").doesNotExist());
        mvc.perform(get(route).param("page","1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content",hasSize(5)));
        mvc.perform(get(route).param("q","alert"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].body").value("<script>alert(1)</script>"));
        mvc.perform(get(route).param("q","%"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(route).param("q","x".repeat(81)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/support/"+ticket.getId()).param("q","alert"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        mvc.perform(get("/admin/support/"+ticket.getId()+"/messages")
                .with(user("api-ticket-staff").roles("STAFF")).param("q","alert"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(route).param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get(route).param("page","10001")).andExpect(status().isBadRequest());
        mvc.perform(get("/support/"+privateTicket.getId()+"/messages"))
            .andExpect(status().isNotFound());
        mvc.perform(get(route).with(user("api-ticket-outsider")))
            .andExpect(status().isNotFound());
        mvc.perform(get(route).with(user("api-ticket-staff").roles("STAFF")))
            .andExpect(status().isNotFound());
        mvc.perform(get("/admin/support/"+ticket.getId()+"/messages")
                .with(user("api-ticket-staff").roles("STAFF")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content",hasSize(20)));
        mvc.perform(get("/admin/support/"+ticket.getId()+"/messages"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadTicketContent() throws Exception {
        mvc.perform(get("/support/1/messages"))
            .andExpect(status().is3xxRedirection());
    }
}
