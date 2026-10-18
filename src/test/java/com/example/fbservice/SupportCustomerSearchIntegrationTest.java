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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:supportCustomerSearch27;DB_CLOSE_DELAY=-1")
class SupportCustomerSearchIntegrationTest {
    @Autowired SupportTicketService tickets;
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="search-customer-staff",roles="STAFF")
    void staffMayFilterByCustomerAndLiteralSpecialCharacters() throws Exception {
        tickets.create("ticket-owner.alpha","Thông tin hỗ trợ A",
            "Kiểm tra dữ liệu theo tài khoản được nhập.");
        tickets.create("ticket-owner_beta","Thông tin hỗ trợ B",
            "Kiểm tra tài khoản có dấu gạch dưới.");
        tickets.create("another-customer","Thông tin hỗ trợ C",
            "Không hiển thị khi tìm khách hàng cụ thể.");
        mvc.perform(get("/admin/support").param("customer","owner.alpha"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(1)))
            .andExpect(content().string(containsString("ticket-owner.alpha")))
            .andExpect(content().string(not(containsString("ticket-owner_beta"))));
        mvc.perform(get("/admin/support").param("customer","_"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(1)))
            .andExpect(content().string(containsString("ticket-owner_beta")));
        mvc.perform(get("/admin/support").param("customer","x".repeat(81)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/support").with(user("search-ordinary-customer"))
                .param("customer","ticket-owner.alpha"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/support").with(user("ticket-owner.alpha"))
                .param("customer","another-customer"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("tickets",hasSize(1)));
    }
}
