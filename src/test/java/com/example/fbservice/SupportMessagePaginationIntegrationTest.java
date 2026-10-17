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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:supportPaging;DB_CLOSE_DELAY=-1")
class SupportMessagePaginationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService service;

    @Test @WithMockUser(username="page-ticket-owner")
    void ownerHistoryIsBoundedAndIsolatedWithStablePages() throws Exception {
        var ticket=service.create("page-ticket-owner","Trợ giúp thông tin",
            "Tôi cần nhận phản hồi về hỗ trợ dịch vụ.");
        for (int i=0;i<45;i++) service.replyAsOwner("page-ticket-owner",ticket.getId(),"Phản hồi số "+i);
        assertThat(service.pageMessagesForOwner("page-ticket-owner",ticket.getId(),0).getContent()).hasSize(20);
        mvc.perform(get("/support/"+ticket.getId()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("messages",hasSize(20)))
            .andExpect(model().attribute("hasNext",true));
        mvc.perform(get("/support/"+ticket.getId()).param("page","2"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("messages",hasSize(5)));
        mvc.perform(get("/support/"+ticket.getId()).param("page","10001"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/support/"+ticket.getId()).with(user("page-ticket-stranger")))
            .andExpect(status().isNotFound());
        mvc.perform(get("/support/"+ticket.getId()).param("page","1").with(user("page-ticket-stranger")))
            .andExpect(status().isNotFound());
        mvc.perform(post("/support/"+ticket.getId()+"/reply").param("message","Không CSRF"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username="page-support-staff",roles="STAFF")
    void staffPagesHistoryAndHtmlPayloadIsEscaped() throws Exception {
        var ticket=service.create("page-support-client","Hỗ trợ bảo mật",
            "Phản hồi về vấn đề bảo mật tài khoản.");
        service.replyAsOwner("page-support-client",ticket.getId(),"<script>alert(1)</script>");
        mvc.perform(get("/admin/support/"+ticket.getId()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                 org.hamcrest.Matchers.containsString("<script>alert(1)</script>"))));
        mvc.perform(get("/admin/support/"+ticket.getId()).param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/admin/support/"+ticket.getId()+"/reply").param("message","Không CSRF"))
            .andExpect(status().isForbidden());
    }
}
