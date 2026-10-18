package com.example.fbservice;

import com.example.fbservice.support.SupportTicketService;
import com.example.fbservice.support.TicketStatus;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:supportReopenW3;DB_CLOSE_DELAY=-1")
class SupportTicketReopenIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;

    @Test
    @WithMockUser(username = "reopen-owner")
    void ownerMayReopenClosedTicketWithPrivateEscapedReasonAndStatusTimeline() throws Exception {
        var ticket = tickets.create("reopen-owner", "Đã xử lý nhưng lỗi còn",
                "Yêu cầu cần được xử lý đúng quy trình hỗ trợ.");
        Long id = ticket.getId();
        String reopen = "/support/" + id + "/reopen";

        // OPEN tickets cannot be reopened and no timeline entries are inserted.
        mvc.perform(post(reopen).with(csrf()).param("reason", "Chưa giải quyết xong"))
            .andExpect(status().isOk());
        assertThat(tickets.detailForOwner("reopen-owner", id).getStatus())
            .isEqualTo(TicketStatus.OPEN);
        assertThat(tickets.messagesForOwner("reopen-owner", id)).isEmpty();

        mvc.perform(post("/admin/support/" + id + "/close")
                .with(user("reopen-staff").roles("STAFF")).with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForOwner("reopen-owner", id).getStatus())
            .isEqualTo(TicketStatus.CLOSED);
        assertThat(tickets.messagesForOwner("reopen-owner", id)).hasSize(1);
        mvc.perform(get("/support/" + id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Mở lại yêu cầu đã đóng")));

        mvc.perform(post(reopen).param("reason", "Chưa giải quyết xong"))
            .andExpect(status().isForbidden());
        mvc.perform(post(reopen).with(user("reopen-other-owner")).with(csrf())
                .param("reason", "Lỗi vẫn còn tồn tại"))
            .andExpect(status().isNotFound());
        mvc.perform(post(reopen).with(csrf()).param("reason", "short"))
            .andExpect(status().isOk());
        assertThat(tickets.detailForOwner("reopen-owner", id).getStatus())
            .isEqualTo(TicketStatus.CLOSED);

        String hostile = "<script>alert('private')</script> Vấn đề vẫn còn xuất hiện.";
        mvc.perform(post(reopen).with(csrf()).param("reason", hostile))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/support/" + id));
        assertThat(tickets.detailForOwner("reopen-owner", id).getStatus())
            .isEqualTo(TicketStatus.OPEN);
        assertThat(tickets.messagesForOwner("reopen-owner", id)).hasSize(3);
        mvc.perform(get("/support/" + id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString(hostile))))
            .andExpect(content().string(not(containsString("Mở lại yêu cầu đã đóng"))));

        // Retry cannot duplicate an OPEN -> OPEN entry.
        mvc.perform(post(reopen).with(csrf()).param("reason", "Vẫn muốn mở lại lần nữa"))
            .andExpect(status().isOk());
        assertThat(tickets.messagesForOwner("reopen-owner", id)).hasSize(3);
    }

    @Test
    @WithMockUser(username = "reopen-owner-noaccess")
    void otherOwnersTicketsCannotBeReopenedOrRead() throws Exception {
        var foreign = tickets.create("reopen-foreign",
                "Sự cố quyền truy cập", "Ticket thuộc tài khoản người dùng khác.");
        mvc.perform(get("/support/" + foreign.getId()))
            .andExpect(status().isNotFound());
        mvc.perform(post("/support/" + foreign.getId() + "/reopen").with(csrf())
                .param("reason", "Cố thử mở lại ticket của người khác"))
            .andExpect(status().isNotFound());
    }
}
