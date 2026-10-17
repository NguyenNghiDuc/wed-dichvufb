package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.payment.PaymentReceiptRepository;
import com.example.fbservice.repository.DepositRequestRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
        "spring.datasource.username=growlab",
        "spring.datasource.password=testpass",
        "server.servlet.session.cookie.secure=false",
        "growth.payments.webhook-secret=test-only-internal-sandbox-key-0123456789"
})
class PostgresPaymentConcurrencyIntegrationTest {
    private static final String SECRET = "test-only-internal-sandbox-key-0123456789";
    @Autowired MockMvc mvc;
    @Autowired DepositRequestRepository deposits;
    @Autowired PaymentReceiptRepository receipts;

    private DepositRequest createDeposit(String owner) {
        DepositRequest request = new DepositRequest();
        request.setAmount(55000);
        request.setOwnerUsername(owner);
        return deposits.save(request);
    }

    private int send(String eventId, long depositId) throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String body = "{\"eventId\":\"" + eventId + "\",\"depositRequestId\":"
                + depositId + ",\"amountVnd\":55000,\"status\":\"SETTLED\"}";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(
                (timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
        return mvc.perform(post("/api/v1/payments/webhooks/internal")
                .header("X-GrowLab-Timestamp", timestamp)
                .header("X-GrowLab-Signature", signature)
                .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
    }

    @Test
    void concurrentDifferentEventsForOneDepositProduceExactlyOneReceipt() throws Exception {
        DepositRequest deposit = createDeposit("pg-concurrent-owner-1");
        String first = "pgRaceSameDepositA";
        String second = "pgRaceSameDepositB";
        int[] statuses = race(() -> send(first, deposit.getId()), () -> send(second, deposit.getId()));
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(deposits.findById(deposit.getId()).orElseThrow().getStatus()).isEqualTo("APPROVED");
        int persisted = (receipts.findByEventId(first).isPresent() ? 1 : 0)
                + (receipts.findByEventId(second).isPresent() ? 1 : 0);
        assertThat(persisted).isEqualTo(1);
    }

    @Test
    void concurrentSameEventIdAcrossDepositsProducesOneApprovedDeposit() throws Exception {
        DepositRequest first = createDeposit("pg-concurrent-owner-2");
        DepositRequest second = createDeposit("pg-concurrent-owner-3");
        String reference = "pgRaceCrossDeposit";
        int[] statuses = race(() -> send(reference, first.getId()),
                () -> send(reference, second.getId()));
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(receipts.findByEventId(reference)).isPresent();
        long approved = java.util.stream.Stream.of(first.getId(), second.getId())
                .map(id -> deposits.findById(id).orElseThrow().getStatus())
                .filter("APPROVED"::equals).count();
        assertThat(approved).isEqualTo(1);
    }

    @FunctionalInterface
    private interface Call { int invoke() throws Exception; }

    private int[] race(Call first, Call second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> a = pool.submit(() -> { start.await(); return first.invoke(); });
            Future<Integer> b = pool.submit(() -> { start.await(); return second.invoke(); });
            start.countDown();
            return new int[]{a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)};
        } finally {
            pool.shutdownNow();
        }
    }
}
