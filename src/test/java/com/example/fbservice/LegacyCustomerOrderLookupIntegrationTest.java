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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:legacycustomerlookup;DB_CLOSE_DELAY=-1")
class LegacyCustomerOrderLookupIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orders;

    private Order create(String owner, String note) {
        Order item = new Order();
        item.setLinkOrder("https://youtube.com/@real");
        item.setServerName("Tư vấn nội dung");
        item.setQuantity(2);
        item.setNote(note);
        return orders.create(item, owner);
    }

    @Test
    @WithMockUser(username="lookup-owner")
    void lookupAndCsvSearchRespectOwnerAndSameIdFilter() throws Exception {
        Order mine = create("lookup-owner", "unique-mine-lookup");
        Order hidden = create("lookup-other", "unique-foreign-lookup");
        create("lookup-owner", "unfiltered-owner-order");
        mvc.perform(get("/order/list").param("orderId", mine.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", hasSize(1)))
            .andExpect(content().string(containsString("unique-mine-lookup")))
            .andExpect(content().string(not(containsString("unique-foreign-lookup"))));
        mvc.perform(get("/order/export.csv").param("orderId", mine.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("unique-mine-lookup")))
            .andExpect(content().string(not(containsString("unfiltered-owner-order"))));
        mvc.perform(get("/order/list").param("orderId", hidden.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", hasSize(0)));
        mvc.perform(get("/order/export.csv").param("orderId", hidden.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("unique-foreign-lookup"))));
        mvc.perform(get("/order/list").with(user("lookup-other"))
                .param("orderId", mine.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", hasSize(0)));
        mvc.perform(get("/order/list").param("orderId", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/order/export.csv").param("orderId", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/order/list").param("orderId", "abc"))
            .andExpect(status().isBadRequest());
    }
}
