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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:legacycustomerorderpaging;DB_CLOSE_DELAY=-1")
class LegacyCustomerOrderPagingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orders;

    private Order create(String owner) {
        Order request = new Order();
        request.setLinkOrder("https://youtube.com/@example");
        request.setServerName("Tư vấn nội bộ");
        request.setQuantity(5);
        request.setPricePerUnit(1);
        return orders.create(request, owner);
    }

    @Test @WithMockUser(username="legacy-list-owner")
    void pagesAreBoundedSortedAndScopedToOwner() throws Exception {
        for (int i=0; i<23; i++) create("legacy-list-owner");
        create("legacy-list-other");
        mvc.perform(get("/order/list").param("page","0"))
            .andExpect(status().isOk()).andExpect(view().name("order_list"))
            .andExpect(model().attribute("orders", hasSize(20)))
            .andExpect(model().attribute("hasNext", true));
        mvc.perform(get("/order/list").param("page","1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", hasSize(3)))
            .andExpect(model().attribute("hasNext", false));
        mvc.perform(get("/order/list").with(user("legacy-list-other")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", hasSize(1)));
        mvc.perform(get("/order/list").with(user("legacy-list-empty")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", hasSize(0)));
        mvc.perform(get("/order/list").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/order/list").param("page","10001"))
            .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(10,create("legacy-list-price").getPricePerUnit());
    }

    @Test void anonymousPageIsProtected() throws Exception {
        mvc.perform(get("/order/list")).andExpect(status().is3xxRedirection());
    }
}
