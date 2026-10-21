package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.WalletCredit;
import com.example.fbservice.payment.WalletCreditRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import com.example.fbservice.service.DepositService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:paymentreconciliation;DB_CLOSE_DELAY=-1")
class AdminPaymentReconciliationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DepositService depositService;
    @Autowired DepositRequestRepository deposits;
    @Autowired WalletCreditRepository wallet;

    DepositRequest create(String owner,long amount,String status){
        DepositRequest input=new DepositRequest();
        input.setAmount(amount);
        var d=depositService.save(input,owner);
        d.setStatus(status);
        return deposits.saveAndFlush(d);
    }

    @Test void reportsUncreditedLegacyApprovalsAndVerifiedCreditsSeparately() throws Exception {
        var legacy=create("legacy_uncredited",65000,"APPROVED");
        var credited=create("verified_credit",75000,"APPROVED");
        create("pending_waiting",80000,"PENDING");
        wallet.saveAndFlush(new WalletCredit("verified_credit",credited.getId(),"casso-reconcile-001",75000));
        var response=mvc.perform(get("/admin/deposits/reconciliation").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(view().name("admin_payment_reconciliation"))
            .andExpect(model().attribute("approvedWithoutCredit",1L))
            .andExpect(model().attribute("pendingCount",1L))
            .andExpect(model().attribute("creditedTotal",75000L))
            .andExpect(model().attribute("creditedCount",1L))
            .andReturn();
        assertThat(response.getResponse().getContentAsString()).contains("legacy_uncredited")
            .doesNotContain("verified_credit</td>");
        assertThat(deposits.findById(legacy.getId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
    }

    @Test void onlyAdminCanInspectReconciliationAndPageMustBeValid() throws Exception {
        mvc.perform(get("/admin/deposits/reconciliation")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/deposits/reconciliation").with(user("customer")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/deposits/reconciliation").with(user("staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/deposits/reconciliation").with(user("admin").roles("ADMIN"))
            .param("page","-1")).andExpect(status().isBadRequest());
    }
}
