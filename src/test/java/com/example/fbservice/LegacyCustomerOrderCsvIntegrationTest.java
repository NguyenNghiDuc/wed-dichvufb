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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:legacyordercsv;DB_CLOSE_DELAY=-1")
class LegacyCustomerOrderCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orders;

    private void save(String owner, String note) {
        Order o = new Order();
        o.setLinkOrder("https://youtube.com/@channel");
        o.setServerName("Tư vấn phát triển kênh");
        o.setQuantity(3);
        o.setPricePerUnit(1);
        o.setNote(note);
        orders.create(o, owner);
    }

    @Test @WithMockUser(username="legacy-csv-owner")
    void csvIsOwnerOnlyServerPricedAndFormulaEscaped() throws Exception {
        save("legacy-csv-owner", "=SUM(1,2)");
        save("legacy-csv-other", "private-value");
        mvc.perform(get("/order/export.csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Content-Disposition", containsString("attachment")))
            .andExpect(content().string(containsString("unit_price_vnd,total_price_vnd")))
            .andExpect(content().string(containsString("\"'=SUM(1,2)\"")))
            .andExpect(content().string(not(containsString("private-value"))))
            .andExpect(content().string(containsString(",\"10\",\"30\",")));
        mvc.perform(get("/order/export.csv").with(user("legacy-csv-other")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("private-value")))
            .andExpect(content().string(not(containsString("=SUM"))));
        mvc.perform(get("/order/list")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Xuất CSV")));
    }

    @Test void anonymousCsvRedirectsToLogin() throws Exception {
        mvc.perform(get("/order/export.csv")).andExpect(status().is3xxRedirection());
    }
}
