package com.example.fbservice;

import com.example.fbservice.model.Order;
import com.example.fbservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:legacyorderdetail;DB_CLOSE_DELAY=-1")
class LegacyCustomerOrderDetailIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orders;

    private Order create(String owner, String note) {
        Order item = new Order();
        item.setServerName("Tư vấn kênh");
        item.setLinkOrder("https://youtube.com/@owner");
        item.setQuantity(7);
        item.setNote(note);
        return orders.create(item, owner);
    }

    @Test @WithMockUser(username="detail-owner")
    void detailIsReadOnlyAndScopedToAuthenticatedOwner() throws Exception {
        Order mine=create("detail-owner", "Khách tự tạo nội dung");
        Order foreign=create("detail-other", "Ghi chú bí mật của người khác");
        mvc.perform(get("/order/" + mine.getId()))
            .andExpect(status().isOk())
            .andExpect(view().name("legacy_order_detail"))
            .andExpect(content().string(containsString("Chi tiết đơn hàng cũ")))
            .andExpect(content().string(containsString("Khách tự tạo nội dung")))
            .andExpect(content().string(containsString("70 VND")))
            .andExpect(content().string(not(containsString("Ghi chú bí mật"))));
        mvc.perform(get("/order/" + foreign.getId()))
            .andExpect(status().isNotFound());
        mvc.perform(get("/order/" + mine.getId()).with(user("detail-other")))
            .andExpect(status().isNotFound());
        mvc.perform(get("/order/99999999")).andExpect(status().isNotFound());
        mvc.perform(get("/order/list")).andExpect(status().isOk())
            .andExpect(content().string(containsString("/order/" + mine.getId())));
    }

    @Test void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/order/1")).andExpect(status().is3xxRedirection());
    }
}
