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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportSearchPriority;DB_CLOSE_DELAY=-1")
class SupportTicketSearchPriorityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SupportTicketService tickets;

    @Test @WithMockUser(username="search-ticket-owner")
    void tenantSearchFiltersByLiteralSubjectAndNeverRevealsOtherOwners() throws Exception {
        var mine=tickets.create("search-ticket-owner","Hỗ trợ % dịch vụ","Khách hàng cần phản hồi đơn tư vấn.");
        tickets.create("search-ticket-owner","Phần mềm ứng dụng","Hỏi về phần mềm xử lý báo cáo.");
        tickets.create("search-ticket-private","Hỗ trợ % riêng","Đây là yêu cầu riêng của khách khác.");
        mvc.perform(get("/support").param("subject","%"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(1)))
            .andExpect(content().string(containsString("Hỗ trợ % dịch vụ")))
            .andExpect(content().string(not(containsString("Hỗ trợ % riêng"))));
        mvc.perform(get("/support").param("subject","ĐẶT HÀNG"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(0)));
        mvc.perform(get("/support").param("status","CLOSED"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(0)));
        mvc.perform(get("/support").param("subject","x".repeat(81)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/support").param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/support/"+mine.getId()).with(user("search-ticket-other")))
            .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(username="priority-ticket-staff",roles="STAFF")
    void staffMaySetPriorityWithCsrfAndFilterQueue() throws Exception {
        var target=tickets.create("priority-ticket-customer","Khẩn cấp tài khoản","Tôi cần hỗ trợ gấp về tài khoản.");
        var second=tickets.create("priority-ticket-other","Yêu cầu bình thường","Thắc mắc về dịch vụ nội dung.");
        String url="/admin/support/"+target.getId()+"/priority";
        mvc.perform(post(url).param("priority","URGENT")).andExpect(status().isForbidden());
        mvc.perform(post(url).with(csrf()).param("priority","WRONG"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(url).with(csrf()).param("priority","URGENT"))
            .andExpect(status().is3xxRedirection());
        assertThat(tickets.detailForAdmin(target.getId()).getPriority()).isEqualTo(TicketPriority.URGENT);
        assertThat(tickets.detailForAdmin(second.getId()).getPriority()).isEqualTo(TicketPriority.NORMAL);
        mvc.perform(get("/admin/support").param("priority","URGENT"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(1)));
        mvc.perform(post(url).with(user("priority-ticket-outsider")).with(csrf())
                .param("priority","LOW")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/support/"+target.getId()+"/close").with(csrf()))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post(url).with(csrf()).param("priority","HIGH"))
            .andExpect(status().isOk());
        assertThat(tickets.detailForAdmin(target.getId()).getPriority()).isEqualTo(TicketPriority.URGENT);
    }

    @Test void anonymousAndOrdinaryUsersCannotSeeStaffQueue() throws Exception {
        mvc.perform(get("/admin/support").with(user("not-staff")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/support")).andExpect(status().is3xxRedirection());
    }
}
