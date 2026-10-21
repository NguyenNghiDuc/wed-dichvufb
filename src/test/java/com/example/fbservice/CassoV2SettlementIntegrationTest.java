package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.WalletCreditRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import com.example.fbservice.service.DepositService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
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
@TestPropertySource(properties={
  "spring.datasource.url=jdbc:h2:mem:cassov2ledger;DB_CLOSE_DELAY=-1",
  "growth.payments.bank.account-number=123456789012",
  "growth.payments.bank.account-holder=GrowLab Test",
  "growth.payments.casso.webhook-secret=0123456789abcdef0123456789abcdef",
  "growth.payments.casso.enabled=true",
  "growth.payments.live-ready=true"
})
class CassoV2SettlementIntegrationTest {
    private static final String SECRET="0123456789abcdef0123456789abcdef";
    @Autowired MockMvc mvc;
    @Autowired DepositService deposits;
    @Autowired DepositRequestRepository depositRepo;
    @Autowired WalletCreditRepository credits;
    @Autowired ObjectMapper json;

    private DepositRequest create(String owner,long amount){
        DepositRequest d=new DepositRequest();
        d.setAmount(amount);return deposits.save(d,owner);
    }
    private String payload(long cassoId,long amount,String account,
                           String bankCode,String description) throws Exception{
        Map<String,Object> inner=new TreeMap<>();
        inner.put("accountNumber",account);
        inner.put("amount",amount);
        inner.put("bankAbbreviation",bankCode);
        inner.put("description",description);
        inner.put("id",cassoId);
        inner.put("reference","FT-"+cassoId);
        inner.put("transactionDateTime","2026-10-21 09:00:00");
        return json.writeValueAsString(Map.of("error",0,"data",inner));
    }
    private String sig(String body) throws Exception {
        long timestamp=Instant.now().toEpochMilli();
        Mac mac=Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
        var node=json.readTree(body);
        String canon=json.writeValueAsString(com.example.fbservice.payment.CassoV2WebhookService.sort(node));
        String value=timestamp+"."+canon;
        return "t="+timestamp+",v1="+HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test void authenticSignedIncomingTransferCreditsOnceAndDedupesProviderId() throws Exception {
        DepositRequest d=create("casso_owner",50000);
        String body=payload(9971501L,50000,"123456789012","TCB","GLNAP"+d.getId());
        String signature=sig(body);
        mvc.perform(post("/api/v1/payments/webhooks/casso-v2").contentType("application/json")
            .content(body)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/payments/webhooks/casso-v2").contentType("application/json")
            .header("X-Casso-Signature",signature.replace("v1=","v1=0")).content(body))
            .andExpect(status().isUnauthorized());
        assertThat(credits.balanceForUser("casso_owner")).isZero();
        mvc.perform(post("/api/v1/payments/webhooks/casso-v2").contentType("application/json")
            .header("X-Casso-Signature",signature).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.duplicate").value(false));
        mvc.perform(post("/api/v1/payments/webhooks/casso-v2").contentType("application/json")
            .header("X-Casso-Signature",signature).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.duplicate").value(true));
        assertThat(credits.balanceForUser("casso_owner")).isEqualTo(50000L);
        assertThat(depositRepo.findById(d.getId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
    }

    @Test void mismatchedAmountsWrongBanksAndFakeDemoEventNeverCredit() throws Exception {
        DepositRequest d=create("casso_bad",82000);
        String ref="GLNAP"+d.getId();
        String[] bad={
          payload(9971502L,81000,"123456789012","TCB",ref),
          payload(9971503L,82000,"999999999999","TCB",ref),
          payload(9971504L,82000,"123456789012","MBB",ref),
          payload(9971505L,-82000,"123456789012","TCB",ref),
          payload(0,82000,"123456789012","TCB",ref),
          payload(9971506L,82000,"123456789012","TCB","NO-REFERENCE")
        };
        for(String body:bad){
            mvc.perform(post("/api/v1/payments/webhooks/casso-v2").contentType("application/json")
                .header("X-Casso-Signature",sig(body)).content(body))
                .andExpect(status().isConflict());
        }
        assertThat(credits.balanceForUser("casso_bad")).isZero();
        assertThat(depositRepo.findById(d.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test void aSingleProviderIdCannotFundAnotherDeposit() throws Exception {
        var first=create("casso_first",90000);
        var second=create("casso_second",90000);
        String a=payload(9971507L,90000,"123456789012","TCB","GLNAP"+first.getId());
        mvc.perform(post("/api/v1/payments/webhooks/casso-v2").contentType("application/json")
            .header("X-Casso-Signature",sig(a)).content(a)).andExpect(status().isOk());
        String b=payload(9971507L,90000,"123456789012","TCB","GLNAP"+second.getId());
        mvc.perform(post("/api/v1/payments/webhooks/casso-v2").contentType("application/json")
            .header("X-Casso-Signature",sig(b)).content(b)).andExpect(status().isConflict());
        assertThat(credits.balanceForUser("casso_second")).isZero();
    }
}
