package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.PaymentReceiptRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:paymentofftest;DB_CLOSE_DELAY=-1",
        "growth.payments.webhook-secret="
})
class PaymentWebhookDisabledIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepositRequestRepository deposits;
    @Autowired PaymentReceiptRepository receipts;

    @Test
    void missingSecretFailsClosedAndNeverChangesDeposit() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setOwnerUsername("payment-disabled-owner");
        request.setAmount(42000);
        request = deposits.save(request);
        long priorReceipts = receipts.count();
        String body = "{\"eventId\":\"disabledevent0001\",\"depositRequestId\":"
                + request.getId() + ",\"amountVnd\":42000,\"status\":\"SETTLED\"}";
        mvc.perform(post("/api/v1/payments/webhooks/internal")
                        .contentType("application/json")
                        .header("X-GrowLab-Timestamp",
                                Long.toString(java.time.Instant.now().getEpochSecond()))
                        .header("X-GrowLab-Signature", "0".repeat(64))
                        .content(body))
                .andExpect(status().isServiceUnavailable());
        assertThat(receipts.count()).isEqualTo(priorReceipts);
        assertThat(deposits.findById(request.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }
}
