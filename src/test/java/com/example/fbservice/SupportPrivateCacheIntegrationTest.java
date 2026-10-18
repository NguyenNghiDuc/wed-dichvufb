package com.example.fbservice;

import org.junit.jupiter.api.Test;
import com.example.fbservice.support.SupportTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportPrivateCache27;DB_CLOSE_DELAY=-1")
class SupportPrivateCacheIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;

    @Test @WithMockUser(username="private-cache-customer")
    void neverCachesAccountScopedChatAndTicketPagesOrJson() throws Exception {
        mvc.perform(get("/chat/ui"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/chat/contacts"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/support"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/chat/thread/private-cache-other"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
        var ticket = tickets.create("private-cache-customer", "Vấn đề riêng tư",
            "Tôi cần nhận phản hồi riêng về yêu cầu hỗ trợ.");
        mvc.perform(get("/support/" + ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void staffTicketQueueAlsoHasPrivateCachePolicy() throws Exception {
        mvc.perform(get("/admin/support").with(user("private-cache-staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
