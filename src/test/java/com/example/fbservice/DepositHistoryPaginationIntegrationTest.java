package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.service.DepositService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:depositpaging;DB_CLOSE_DELAY=-1")
class DepositHistoryPaginationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepositService service;

    @Test
    @WithMockUser(username = "deposit-page-owner", roles = "USER")
    void historyIsOwnerScopedBoundedAndNavigableOnMobile() throws Exception {
        for (int i = 0; i < 22; i++) {
            DepositRequest item = new DepositRequest();
            item.setAmount(20000 + i);
            service.save(item, "deposit-page-owner");
        }
        DepositRequest other = new DepositRequest();
        other.setAmount(999900);
        service.save(other, "other-deposit-owner");

        mvc.perform(get("/deposit/list"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("deposits", hasSize(20)))
                .andExpect(model().attribute("totalElements", 22L))
                .andExpect(model().attribute("hasNext", true))
                .andExpect(content().string(containsString("page=1")))
                .andExpect(content().string(not(containsString("999900"))));
        mvc.perform(get("/deposit/list").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("deposits", hasSize(2)))
                .andExpect(model().attribute("hasPrevious", true))
                .andExpect(model().attribute("hasNext", false));
    }

    @Test
    @WithMockUser(username = "deposit-page-invalid", roles = "USER")
    void badPagesAreRejectedAndLargeOffsetsAreBounded() throws Exception {
        mvc.perform(get("/deposit/list").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/deposit/list").param("page", "10001"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/deposit/list").param("page", "not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousHistoryIsNotAccessible() throws Exception {
        mvc.perform(get("/deposit/list")).andExpect(status().is3xxRedirection());
    }
}
