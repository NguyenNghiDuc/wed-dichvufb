package com.example.fbservice;

import com.example.fbservice.model.*;
import com.example.fbservice.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:hardeningtest;DB_CLOSE_DELAY=-1")
class InputHardeningTest {
    @Autowired MockMvc mvc;
    @Autowired DepositService deposits;
    @Autowired OrderService orders;

    @Test @WithMockUser(username="limituser")
    void refuseOversizeMessageAndRegistrationEmail() throws Exception {
        mvc.perform(post("/chat/send").with(csrf()).contentType("application/json")
                .content("{\"receiver\":\"staff\",\"content\":\""+ "x".repeat(300)+"\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/register").with(csrf())
            .param("username","newuser_longmail")
            .param("password","passphrase123")
            .param("email","a".repeat(300)+"@example.org"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"));
    }

    @Test
    void refuseOversizedLegacyFields() {
        DepositRequest d=new DepositRequest();
        d.setAmount(20000);
        d.setNote("x".repeat(300));
        assertThatThrownBy(()->deposits.save(d,"someone")).isInstanceOf(IllegalArgumentException.class);
        Order o=new Order();
        o.setLinkOrder("https://example.com");
        o.setQuantity(10);
        o.setServerName("x".repeat(300));
        assertThatThrownBy(()->orders.create(o,"someone")).isInstanceOf(IllegalArgumentException.class);
    }
}
