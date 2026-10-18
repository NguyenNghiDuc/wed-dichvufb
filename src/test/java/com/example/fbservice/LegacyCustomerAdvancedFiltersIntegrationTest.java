package com.example.fbservice;

import com.example.fbservice.model.Order;
import com.example.fbservice.service.OrderService;
import java.time.LocalDate;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:legacyadvancedfilters;DB_CLOSE_DELAY=-1")
class LegacyCustomerAdvancedFiltersIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orders;

    private Order add(String owner, String serviceName, String note) {
        Order o=new Order();
        o.setLinkOrder("https://youtube.com/@channel");
        o.setServerName(serviceName);
        o.setQuantity(2);
        o.setNote(note);
        return orders.create(o,owner);
    }

    @Test @WithMockUser(username="legacy-filter-owner")
    void serviceDateIdAndCsvFiltersStayOwnerScoped() throws Exception {
        Order mine=add("legacy-filter-owner","Tư vấn nội dung","mine-filter");
        Order another=add("legacy-filter-owner","Tư vấn quảng cáo","other-service");
        Order foreign=add("legacy-filter-foreign","Tư vấn nội dung","foreign-filter");
        String from=LocalDate.now().minusDays(1).toString();
        String to=LocalDate.now().plusDays(1).toString();
        mvc.perform(get("/order/list").param("service","Tư vấn nội dung")
            .param("from",from).param("to",to))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orders", hasSize(1)))
            .andExpect(content().string(containsString("mine-filter")))
            .andExpect(content().string(not(containsString("other-service"))))
            .andExpect(content().string(not(containsString("foreign-filter"))));
        mvc.perform(get("/order/export.csv").param("service","Tư vấn nội dung")
            .param("from",from).param("to",to))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("mine-filter")))
            .andExpect(content().string(not(containsString("other-service"))))
            .andExpect(content().string(not(containsString("foreign-filter"))));
        mvc.perform(get("/order/list").with(user("legacy-filter-foreign"))
            .param("orderId",mine.getId().toString()))
            .andExpect(model().attribute("orders", hasSize(0)));
        mvc.perform(get("/order/list").param("orderId",foreign.getId().toString()))
            .andExpect(model().attribute("orders", hasSize(0)));
        mvc.perform(get("/order/list").param("service","Tư vấn quảng cáo")
            .param("orderId",another.getId().toString()))
            .andExpect(model().attribute("orders",hasSize(1)));
    }

    @Test @WithMockUser(username="legacy-filter-bad")
    void invalidDateAndServiceAreRejected() throws Exception {
        String before=LocalDate.now().minusDays(1).toString();
        String after=LocalDate.now().plusDays(1).toString();
        mvc.perform(get("/order/list").param("from",after).param("to",before))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/order/export.csv").param("from",after).param("to",before))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/order/list").param("from","not-date"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/order/list").param("service","x".repeat(256)))
            .andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotReadFilteredRecords() throws Exception {
        mvc.perform(get("/order/list").param("service","Tư vấn nội dung"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/order/export.csv").param("service","Tư vấn nội dung"))
            .andExpect(status().is3xxRedirection());
    }
}
