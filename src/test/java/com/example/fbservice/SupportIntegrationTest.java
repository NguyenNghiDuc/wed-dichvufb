package com.example.fbservice;

import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:supporttest;DB_CLOSE_DELAY=-1")
class SupportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;
    @Autowired AccountNotificationRepository notifications;

    @Test
    void anonymousCannotCreateSupportRequest() throws Exception {
        mvc.perform(post("/support").with(csrf())
             .param("subject", "Help needed").param("description", "Important issue with my account"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username="support-owner")
    void customerCreatesAndCannotAccessOthersOrStaffActions() throws Exception {
        var ticket=tickets.create("support-owner","Vấn đề đặt chiến dịch",
            "Không thấy cập nhật tiến độ của chiến dịch tư vấn.");
        mvc.perform(get("/support")).andExpect(status().isOk());
        mvc.perform(get("/support/"+ticket.getId())).andExpect(status().isOk());
        mvc.perform(get("/support/"+ticket.getId()).with(user("outsider")))
            .andExpect(status().isNotFound());
        mvc.perform(post("/support/"+ticket.getId()+"/reply")
            .param("message","Tôi cần thêm thông tin."))
            .andExpect(status().isForbidden());
        mvc.perform(post("/support/"+ticket.getId()+"/reply")
            .with(user("outsider")).with(csrf()).param("message","Attempt impersonation"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/support/"+ticket.getId()+"/reply")
            .with(csrf()).param("message","Tôi cần thêm thông tin."))
            .andExpect(status().is3xxRedirection());
        assertThat(tickets.messagesForOwner("support-owner",ticket.getId())).hasSize(1);
        mvc.perform(get("/admin/support")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="support-moderator",roles="ADMIN")
    void staffRepliesAndClosesWithNotification() throws Exception {
        var ticket=tickets.create("support-owner2","Yêu cầu hỗ trợ dịch vụ",
            "Cần tư vấn giải pháp nội dung cho kênh mới.");
        mvc.perform(get("/admin/support/"+ticket.getId())).andExpect(status().isOk());
        mvc.perform(post("/admin/support/"+ticket.getId()+"/reply").with(csrf())
                .param("message","Chúng tôi đã tiếp nhận và xử lý."))
            .andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus()).isEqualTo(TicketStatus.ANSWERED);
        mvc.perform(post("/admin/support/"+ticket.getId()+"/close").with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("support-owner2")).isEqualTo(3L);
        mvc.perform(post("/admin/support/"+ticket.getId()+"/reply").with(csrf())
            .param("message","Cannot reopen"))
            .andExpect(status().isOk());
    }
    @Test
    void ownerCanReopenWithReasonAndHistoryButNotAnotherOwnersTicket() throws Exception {
        var ticket=tickets.create("reopen-owner","Vấn đề chưa giải quyết","Cần trao đổi lại thông tin về chiến dịch.");
        tickets.closeAsOwner("reopen-owner",ticket.getId());
        String path="/support/"+ticket.getId()+"/reopen";
        mvc.perform(post(path).with(user("reopen-owner")).param("reason","Cần kiểm tra thêm tiến độ xử lý."))
            .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("outsider")).with(csrf()).param("reason","Cần kiểm tra thêm tiến độ xử lý."))
            .andExpect(status().isNotFound());
        mvc.perform(post(path).with(user("reopen-owner")).with(csrf()).param("reason","Cần kiểm tra thêm tiến độ xử lý."))
            .andExpect(redirectedUrl("/support/"+ticket.getId())).andExpect(flash().attributeExists("success"));
        assertThat(tickets.detailForOwner("reopen-owner",ticket.getId()).getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(tickets.messagesForOwner("reopen-owner",ticket.getId())).hasSize(1)
            .allSatisfy(m->assertThat(m.getBody()).contains("[Mở lại yêu cầu]"));
        mvc.perform(post(path).with(user("reopen-owner")).with(csrf()).param("reason","Không mở lại lần hai khi đang mở."))
            .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
        assertThat(tickets.messagesForOwner("reopen-owner",ticket.getId())).hasSize(1);
        tickets.replyAsAdmin("helper",ticket.getId(),"Đã kiểm tra lại nội dung yêu cầu.");
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus()).isEqualTo(TicketStatus.ANSWERED);
    }

    @Test
    void invalidReopenReasonKeepsClosedStateAndShowsFeedback() throws Exception {
        var ticket=tickets.create("reason-owner","Kiểm tra lý do mở lại","Cần kiểm tra thông tin mở lại phiếu hỗ trợ.");
        tickets.closeAsOwner("reason-owner",ticket.getId());
        mvc.perform(post("/support/"+ticket.getId()+"/reopen").with(user("reason-owner")).with(csrf()).param("reason","ngắn"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lý do mở lại")));
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(tickets.messagesForOwner("reason-owner",ticket.getId())).isEmpty();
    }

    @Test
    void staffMayReopenAndCustomerCannotUseAdministrationEndpoint() throws Exception {
        var ticket=tickets.create("staff-reopen-owner","Thông tin phản hồi mới","Có cập nhật cần được nhân viên xem xét lại.");
        tickets.closeAsOwner("staff-reopen-owner",ticket.getId());
        String path="/admin/support/"+ticket.getId()+"/reopen";
        mvc.perform(post(path).with(user("staff-reopen-owner")).with(csrf()).param("reason","Đã có thông tin mới để xem xét."))
            .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("support-staff").roles("STAFF")).with(csrf()).param("reason","Đã có thông tin mới để xem xét."))
            .andExpect(redirectedUrl("/admin/support/"+ticket.getId()));
        assertThat(tickets.detailForAdmin(ticket.getId()).getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(notifications.countByOwnerUsernameAndReadAtIsNull("staff-reopen-owner")).isEqualTo(3);
        mvc.perform(get("/admin/support/"+ticket.getId()).with(user("support-staff").roles("STAFF")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Nhân viên hỗ trợ")));
    }

}
