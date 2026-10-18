package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.PaymentReceipt;
import com.example.fbservice.payment.PaymentReceiptRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:paymentdays;DB_CLOSE_DELAY=-1")
class PaymentDailySummaryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepositRequestRepository deposits;
    @Autowired PaymentReceiptRepository receipts;
    @Autowired JdbcTemplate jdbc;

    private void seed(String eventId, long amount, LocalDate date) {
        DepositRequest request = new DepositRequest();
        request.setOwnerUsername("sensitive-daily-owner");
        request.setAmount(amount);
        request.setStatus("APPROVED");
        request = deposits.saveAndFlush(request);
        receipts.saveAndFlush(new PaymentReceipt(eventId, request.getId(), amount));
        jdbc.update("UPDATE payment_receipts SET created_at = ? WHERE event_id = ?",
                Timestamp.valueOf(date.atTime(12, 0)), eventId);
    }

    @Test
    @WithMockUser(username = "payment-daily-admin", roles = "ADMIN")
    void aggregatesSignedSandboxReceiptsAndPadsMissingDaysWithoutLeakingIdentity() throws Exception {
        LocalDate today = LocalDate.now();
        seed("dailySandboxEvtA2026", 42000, today);
        seed("dailySandboxEvtB2026", 18000, today);
        seed("dailySandboxEvtOld2026", 99000, today.minusDays(3));
        long before = receipts.count();
        mvc.perform(get("/api/v1/admin/payments/reconciliation/daily").param("days", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.source").value("SIGNED_INTERNAL_TEST_EVENTS_ONLY"))
                .andExpect(jsonPath("$.notFinancialReconciliation").value(true))
                .andExpect(jsonPath("$.days").value(2))
                .andExpect(jsonPath("$.daily.length()").value(2))
                .andExpect(jsonPath("$.daily[0].date").value(today.toString()))
                .andExpect(jsonPath("$.daily[0].internalReceiptCount").value(2))
                .andExpect(jsonPath("$.daily[0].internalAmountVnd").value(60000))
                .andExpect(jsonPath("$.daily[1].date").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$.daily[1].internalReceiptCount").value(0))
                .andExpect(jsonPath("$.daily[1].internalAmountVnd").value(0))
                .andExpect(content().string(not(containsString("sensitive-daily-owner"))))
                .andExpect(content().string(not(containsString("dailySandboxEvtA2026"))));
        assertThat(receipts.count()).isEqualTo(before);
    }

    @Test
    @WithMockUser(username = "payment-daily-admin", roles = "ADMIN")
    void rejectsDaysBeyondBoundAndMalformedDays() throws Exception {
        mvc.perform(get("/api/v1/admin/payments/reconciliation/daily").param("days", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/payments/reconciliation/daily").param("days", "32"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/payments/reconciliation/daily").param("days", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "payment-daily-user", roles = "USER")
    void customerCannotReadDailyOperationalTotals() throws Exception {
        mvc.perform(get("/api/v1/admin/payments/reconciliation/daily"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadDailyOperationalTotals() throws Exception {
        mvc.perform(get("/api/v1/admin/payments/reconciliation/daily"))
                .andExpect(status().is3xxRedirection());
    }
}
