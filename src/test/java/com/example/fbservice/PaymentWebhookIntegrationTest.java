package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.repository.DepositRequestRepository;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:paymenttest;DB_CLOSE_DELAY=-1",
    "growth.payments.webhook-secret=0123456789abcdef0123456789abcdef"})
class PaymentWebhookIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepositRequestRepository deposits;
    private static final String SECRET="0123456789abcdef0123456789abcdef";
    private String sign(String body) throws Exception{
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
    @Test
    void rejectsInvalidSignatureAndAmountAndDeduplicatesSettledEvent() throws Exception{
        DepositRequest d=new DepositRequest();d.setOwnerUsername("pay-owner");d.setAmount(50000L);
        d=deposits.save(d);
        String body="{\"eventId\":\"event12345\",\"depositRequestId\":"+d.getId()
            +",\"amountVnd\":50000,\"status\":\"SETTLED\"}";
        mvc.perform(post("/api/v1/payments/webhooks/internal").contentType("application/json")
            .header("X-GrowLab-Signature","0".repeat(64)).content(body))
            .andExpect(status().isUnauthorized());
        assertThat(deposits.findById(d.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        String badAmount=body.replace("50000","40000");
        mvc.perform(post("/api/v1/payments/webhooks/internal").contentType("application/json")
            .header("X-GrowLab-Signature",sign(badAmount)).content(badAmount))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/payments/webhooks/internal").contentType("application/json")
            .header("X-GrowLab-Signature",sign(body)).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.duplicate").value(false));
        mvc.perform(post("/api/v1/payments/webhooks/internal").contentType("application/json")
            .header("X-GrowLab-Signature",sign(body)).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.duplicate").value(true));
        assertThat(deposits.findById(d.getId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
    }
}
