package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.PaymentReceipt;
import com.example.fbservice.payment.PaymentReceiptRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:paymentreview;DB_CLOSE_DELAY=-1"
})
class PaymentReconciliationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepositRequestRepository deposits;
    @Autowired PaymentReceiptRepository receipts;

    @Test
    @WithMockUser(username = "reconcile-admin", roles = "ADMIN")
    void adminGetsReadOnlyAnomalyCountsWithoutSensitiveDetails() throws Exception {
        long beforeReceipts = receipts.count();
        DepositRequest missing = new DepositRequest();
        missing.setOwnerUsername("reconcile-account-private");
        missing.setAmount(21000);
        missing.setStatus("APPROVED");
        deposits.save(missing);

        DepositRequest mismatched = new DepositRequest();
        mismatched.setOwnerUsername("reconcile-account-mismatched");
        mismatched.setAmount(22000);
        mismatched.setStatus("PENDING");
        mismatched = deposits.save(mismatched);
        receipts.save(new PaymentReceipt("reviewEvent000012", mismatched.getId(), 22000));

        mvc.perform(get("/api/v1/admin/payments/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.source").value("SIGNED_INTERNAL_TEST_EVENTS_ONLY"))
                .andExpect(jsonPath("$.notFinancialReconciliation").value(true))
                .andExpect(jsonPath("$.requiresManualReview").value(true))
                .andExpect(jsonPath("$.approvedWithoutReceipt").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.inconsistentReceipts").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("reconcile-account-private"))));
        assertThat(receipts.count()).isEqualTo(beforeReceipts + 1);
        assertThat(deposits.findById(missing.getId()).orElseThrow().getStatus())
                .isEqualTo("APPROVED");
    }

    @Test
    @WithMockUser(username = "reconcile-customer", roles = "USER")
    void customerCannotReadReconciliation() throws Exception {
        mvc.perform(get("/api/v1/admin/payments/reconciliation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "reconcile-staff", roles = "STAFF")
    void staffCannotReadReconciliation() throws Exception {
        mvc.perform(get("/api/v1/admin/payments/reconciliation"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadReconciliation() throws Exception {
        mvc.perform(get("/api/v1/admin/payments/reconciliation"))
                .andExpect(status().is3xxRedirection());
    }
}
