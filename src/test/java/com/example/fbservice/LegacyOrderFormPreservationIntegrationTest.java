package com.example.fbservice;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:legacyformpreservation;DB_CLOSE_DELAY=-1")
class LegacyOrderFormPreservationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrderService orders;

    @Test @WithMockUser(username="legacy-form-owner")
    void invalidFormPreservesEscapedInputAndNoClientPriceIsTrusted() throws Exception {
        mvc.perform(post("/order/save").with(csrf())
                .param("linkOrder", "https://example.com/test")
                .param("serverName", "Hỗ trợ kênh")
                .param("quantity", "0")
                .param("note", "<b>Ghi chú riêng</b>")
                .param("pricePerUnit", "1"))
            .andExpect(status().isOk())
            .andExpect(view().name("order_form"))
            .andExpect(content().string(containsString("https://example.com/test")))
            .andExpect(content().string(containsString("Hỗ trợ k&ecirc;nh")))
            .andExpect(content().string(containsString("&lt;b&gt;Ghi ch&uacute; ri&ecirc;ng&lt;/b&gt;")))
            .andExpect(content().string(not(containsString("<b>Ghi chú riêng</b>"))));
        mvc.perform(post("/order/save").with(csrf())
                .param("linkOrder", "https://example.com/test")
                .param("serverName", "Hỗ trợ kênh")
                .param("quantity", "3").param("note", "bình thường")
                .param("pricePerUnit", "1").param("ownerUsername", "another-owner"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/order/list"));
        var page = orders.pageMine("legacy-form-owner", 0, 20);
        org.junit.jupiter.api.Assertions.assertEquals(1, page.getTotalElements());
        org.junit.jupiter.api.Assertions.assertEquals(10, page.getContent().get(0).getPricePerUnit());
        org.junit.jupiter.api.Assertions.assertEquals(30, page.getContent().get(0).getTotalPrice());
        org.junit.jupiter.api.Assertions.assertEquals("legacy-form-owner", page.getContent().get(0).getOwnerUsername());
    }

    @Test void loginAndCsrfAreRequired() throws Exception {
        mvc.perform(get("/order/new")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/order/save").param("linkOrder","https://example.com")
                .param("quantity","1")).andExpect(status().isForbidden());
    }
}
