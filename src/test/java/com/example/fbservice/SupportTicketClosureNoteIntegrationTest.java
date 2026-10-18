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
        "spring.datasource.url=jdbc:h2:mem:w3resolutionnote;DB_CLOSE_DELAY=-1")
class SupportTicketClosureNoteIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;

    @Test
    @WithMockUser(username = "note-reviewer", roles = "STAFF")
    void closureNoteIsPrivateAndEscapedInCustomerHistory() throws Exception {
        var ticket = tickets.create("note-customer", "Kết thúc yêu cầu hỗ trợ",
                "Tôi muốn biết nội dung đã giải quyết để xem lại.");
        String url = "/admin/support/" + ticket.getId() + "/close";
        String note = "<script>alert('xss')</script> Đã kiểm tra và giải quyết.";
        mvc.perform(post(url).param("resolutionNote", note))
                .andExpect(status().isForbidden()); // normal CSRF protection
        mvc.perform(post(url).with(user("note-customer").roles("USER"))
                .with(csrf()).param("resolutionNote", note))
                .andExpect(status().isForbidden());
        assertThat(tickets.detailForOwner("note-customer", ticket.getId()).getStatus())
                .isEqualTo(TicketStatus.OPEN);
        mvc.perform(post(url).with(csrf()).param("resolutionNote", "ngắn"))
                .andExpect(status().isOk());
        assertThat(tickets.messagesForOwner("note-customer", ticket.getId())).isEmpty();

        mvc.perform(post(url).with(csrf()).param("resolutionNote", note))
                .andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForOwner("note-customer", ticket.getId()).getStatus())
                .isEqualTo(TicketStatus.CLOSED);
        var timeline = tickets.messagesForOwner("note-customer", ticket.getId());
        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(1).getAuthor()).isEqualTo("note-reviewer");
        assertThat(timeline.get(1).getBody()).isEqualTo(note);
        mvc.perform(get("/support/" + ticket.getId())
                    .with(user("note-customer")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString(note))));
        mvc.perform(get("/support/" + ticket.getId())
                    .with(user("another-customer")))
                .andExpect(status().isNotFound());
    }
}
