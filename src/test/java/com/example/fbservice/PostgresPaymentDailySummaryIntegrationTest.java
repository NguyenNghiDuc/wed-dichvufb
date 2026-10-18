package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.PaymentReceipt;
import com.example.fbservice.payment.PaymentReceiptRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
        "spring.datasource.username=growlab",
        "spring.datasource.password=testpass",
        "server.servlet.session.cookie.secure=false"
})
class PostgresPaymentDailySummaryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepositRequestRepository deposits;
    @Autowired PaymentReceiptRepository receipts;
    @Autowired JdbcTemplate jdbc;

    @Test
    @WithMockUser(username = "postgres-daily-admin", roles = "ADMIN")
    void postgresGroupByDateUsesCreatedAtIndexAndReportsSandboxOnly() throws Exception {
        String id = "pgDailyW6Receipt" + System.nanoTime();
        DepositRequest deposit = new DepositRequest();
        deposit.setOwnerUsername("pg-daily-secret-owner");
        deposit.setStatus("APPROVED");
        deposit.setAmount(88000);
        deposit = deposits.saveAndFlush(deposit);
        receipts.saveAndFlush(new PaymentReceipt(id, deposit.getId(), 88000));
        LocalDate today = LocalDate.now();
        long beforeReceipts = receipts.count();
        mvc.perform(get("/api/v1/admin/payments/reconciliation/daily").param("days", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily[0].date").value(today.toString()))
                .andExpect(jsonPath("$.daily[0].internalReceiptCount").isNumber())
                .andExpect(jsonPath("$.daily[0].internalAmountVnd").isNumber())
                .andExpect(jsonPath("$.notFinancialReconciliation").value(true));
        assertThat(receipts.count()).isEqualTo(beforeReceipts);
        Integer indexCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes WHERE schemaname = current_schema()
                AND tablename = 'payment_receipts'
                AND indexname = 'idx_payment_receipts_created_at'
                """, Integer.class);
        assertThat(indexCount).isEqualTo(1);
    }
}
