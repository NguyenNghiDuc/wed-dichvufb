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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:legacyordersummary;DB_CLOSE_DELAY=-1")
class LegacyCustomerOrderSummaryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orders;

    void add(String owner, int quantity) {
        Order order = new Order();
        order.setLinkOrder("https://youtube.com/@channel");
        order.setServerName("Kế hoạch nội dung");
        order.setQuantity(quantity);
        orders.create(order,owner);
    }

    @Test @WithMockUser(username="summary-owner")
    void summaryCountsAllPagesButNeverOtherOwners() throws Exception {
        add("summary-owner", 3);
        add("summary-owner", 7);
        add("summary-other", 500);
        mvc.perform(get("/api/v1/customer/legacy-orders/summary")
                .param("owner","summary-other"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.orderCount").value(2))
            .andExpect(jsonPath("$.amountVnd").value(100));
        mvc.perform(get("/api/v1/customer/legacy-orders/summary").with(user("summary-other")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderCount").value(1))
            .andExpect(jsonPath("$.amountVnd").value(5000));
        mvc.perform(get("/api/v1/customer/legacy-orders/summary").with(user("summary-empty")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderCount").value(0))
            .andExpect(jsonPath("$.amountVnd").value(0));
        mvc.perform(get("/order/list").param("page","0"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("summary"))
            .andExpect(content().string(containsString("Tổng giá trị dự kiến")))
            .andExpect(content().string(not(containsString("5000 đ"))));
    }

    @Test void anonymousCannotGetSummary() throws Exception {
        mvc.perform(get("/api/v1/customer/legacy-orders/summary"))
            .andExpect(status().is3xxRedirection());
    }
}
