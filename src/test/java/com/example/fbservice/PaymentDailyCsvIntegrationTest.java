package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.PaymentReceipt;
import com.example.fbservice.payment.PaymentReceiptRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:paymentcsv;DB_CLOSE_DELAY=-1")
class PaymentDailyCsvIntegrationTest {
    private static final String PATH = "/api/v1/admin/payments/reconciliation/daily.csv";
    @Autowired MockMvc mvc;
    @Autowired DepositRequestRepository deposits;
    @Autowired PaymentReceiptRepository receipts;

    @Test
    @WithMockUser(username = "csv-admin", roles = "ADMIN")
    void adminDownloadsBoundedAggregateCsvWithoutSensitiveColumns() throws Exception {
        DepositRequest deposit = new DepositRequest();
        deposit.setOwnerUsername("csv-sensitive-private-owner");
        deposit.setAmount(65000);
        deposit.setStatus("APPROVED");
        deposit = deposits.saveAndFlush(deposit);
        receipts.saveAndFlush(new PaymentReceipt("csvSensitiveEvent2026", deposit.getId(), 65000));
        mvc.perform(get(PATH).param("days", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=")))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("text/csv;charset=UTF-8")))
                .andExpect(content().string(containsString(
                        "date,internal_receipts,internal_amount_vnd")))
                .andExpect(content().string(containsString(",65000")))
                .andExpect(content().string(not(containsString("csv-sensitive-private-owner"))))
                .andExpect(content().string(not(containsString("csvSensitiveEvent2026"))));
    }

    @Test
    @WithMockUser(username = "csv-admin", roles = "ADMIN")
    void invalidDaysDoNotProduceCsv() throws Exception {
        mvc.perform(get(PATH).param("days", "0")).andExpect(status().isBadRequest());
        mvc.perform(get(PATH).param("days", "32")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "csv-staff", roles = "STAFF")
    void staffCannotDownloadAdminReport() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotDownloadAdminReport() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().is3xxRedirection());
    }
}
