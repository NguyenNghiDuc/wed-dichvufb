package com.example.fbservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:paymentreviewpage;DB_CLOSE_DELAY=-1")
class PaymentReviewPageIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "sandbox-review-admin", roles = "ADMIN")
    void adminRendersResponsiveReadOnlyReviewWithNoStore() throws Exception {
        mvc.perform(get("/admin/payments/review").param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment_review"))
                .andExpect(model().attribute("days", 14))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("Đối soát thử nghiệm")))
                .andExpect(content().string(containsString("Tổng VND thử nghiệm")))
                .andExpect(content().string(containsString("không phải doanh thu")));
    }

    @Test
    @WithMockUser(username = "sandbox-review-customer", roles = "USER")
    void customerCannotLoadSandboxReviewPage() throws Exception {
        mvc.perform(get("/admin/payments/review"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotLoadSandboxReviewPage() throws Exception {
        mvc.perform(get("/admin/payments/review"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "sandbox-review-admin", roles = "ADMIN")
    void invalidLookbackReturnsBadRequest() throws Exception {
        mvc.perform(get("/admin/payments/review").param("days", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/payments/review").param("days", "32"))
                .andExpect(status().isBadRequest());
    }
}
