package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.WalletCreditRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import com.example.fbservice.service.DepositService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties={
  "spring.datasource.url=jdbc:h2:mem:banksettlements;DB_CLOSE_DELAY=-1",
  "growth.payments.bank.account-number=123456789012",
  "growth.payments.bank.account-holder=GrowLab Test",
  "growth.payments.bank-webhook-secret=0123456789abcdef0123456789abcdef",
  "growth.payments.internal-verified-enabled=true"
})
class VerifiedBankSettlementIntegrationTest {
    private static final String SECRET="0123456789abcdef0123456789abcdef";
    @Autowired MockMvc mvc;
    @Autowired DepositService service;
    @Autowired DepositRequestRepository deposits;
    @Autowired WalletCreditRepository credits;
    private String sign(String body) throws Exception {
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
    private DepositRequest deposit(String owner,long amount){
        DepositRequest d=new DepositRequest();d.setAmount(amount);
        return service.save(d,owner);
    }
    private String event(String transactionId,long amount,String destination,String ref){
        return "{\"eventId\":\"event-001-"+transactionId+"\",\"transactionId\":\""+transactionId
            +"\",\"amountVnd\":"+amount+",\"destinationAccount\":\""+destination
            +"\",\"description\":\""+ref+"\",\"status\":\"SETTLED\"}";
    }
    @Test void onlySignedMatchedTransferCreditsWalletExactlyOnce() throws Exception {
        DepositRequest d=deposit("bank_alice",50000);
        String body=event("txn-bank-00001",50000,"123456789012","GLNAP"+d.getId());
        mvc.perform(post("/api/v1/payments/webhooks/bank-settled").contentType("application/json")
            .content(body)).andExpect(status().isUnauthorized());
        assertThat(credits.balanceForUser("bank_alice")).isZero();
        mvc.perform(post("/api/v1/payments/webhooks/bank-settled").contentType("application/json")
            .header("X-GrowLab-Signature",sign(body)).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.duplicate").value(false));
        mvc.perform(post("/api/v1/payments/webhooks/bank-settled").contentType("application/json")
            .header("X-GrowLab-Signature",sign(body)).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.duplicate").value(true));
        assertThat(credits.balanceForUser("bank_alice")).isEqualTo(50000L);
        assertThat(credits.balanceForUser("bank_bob")).isZero();
        assertThat(deposits.findById(d.getId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
        mvc.perform(get("/wallet").with(user("bank_alice")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("50.000 đ")));
        mvc.perform(get("/wallet").with(user("bank_bob")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("50.000 đ"))));
    }
    @Test void wrongAccountWrongAmountAndReferenceNeverCredit() throws Exception {
        DepositRequest d=deposit("bank_invalid",81000);
        for(String body:new String[]{
            event("txn-bank-00002",81000,"999999999999","GLNAP"+d.getId()),
            event("txn-bank-00003",79000,"123456789012","GLNAP"+d.getId()),
            event("txn-bank-00004",81000,"123456789012","SOME-OTHER-TEXT")
        }){
            mvc.perform(post("/api/v1/payments/webhooks/bank-settled").contentType("application/json")
                .header("X-GrowLab-Signature",sign(body)).content(body))
                .andExpect(status().is4xxClientError());
        }
        assertThat(credits.balanceForUser("bank_invalid")).isZero();
        assertThat(deposits.findById(d.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
    }
    @Test void unrelatedValidBankTransactionCannotCreditSecondDeposit() throws Exception {
        DepositRequest one=deposit("bank_first",70000), two=deposit("bank_second",70000);
        String first=event("txn-bank-00005",70000,"123456789012","GLNAP"+one.getId());
        mvc.perform(post("/api/v1/payments/webhooks/bank-settled").contentType("application/json")
            .header("X-GrowLab-Signature",sign(first)).content(first))
            .andExpect(status().isOk());
        String stolen=event("txn-bank-00005",70000,"123456789012","GLNAP"+two.getId());
        mvc.perform(post("/api/v1/payments/webhooks/bank-settled").contentType("application/json")
            .header("X-GrowLab-Signature",sign(stolen)).content(stolen))
            .andExpect(status().isConflict());
        assertThat(credits.balanceForUser("bank_first")).isEqualTo(70000L);
        assertThat(credits.balanceForUser("bank_second")).isZero();
    }
}
