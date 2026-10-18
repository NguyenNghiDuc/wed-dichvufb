package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:customerquoteform;DB_CLOSE_DELAY=-1")
class CustomerQuoteFormIntegrationTest {
    @Autowired MockMvc mvc;

    @Test @WithMockUser(username="form-quote-user")
    void formLoadsExternalCspCompatibleQuotePreviewAndStaticScript() throws Exception {
        mvc.perform(get("/campaigns/new"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/customer-quote.js")))
            .andExpect(content().string(containsString("id=\"quotePreview\"")))
            .andExpect(content().string(containsString("aria-live=\"polite\"")));
        mvc.perform(get("/customer-quote.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("api/v1/campaign-orders/quote")))
            .andExpect(content().string(containsString("textContent")));
    }

    @Test void noAnonymousCustomerDataAccess() throws Exception {
        mvc.perform(get("/campaigns/new")).andExpect(status().is3xxRedirection());
    }
}
