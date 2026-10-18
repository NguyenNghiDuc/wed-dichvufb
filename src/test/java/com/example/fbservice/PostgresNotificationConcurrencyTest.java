package com.example.fbservice;

import com.example.fbservice.notification.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** PostgreSQL 16 CI: race the actual owner-scoped UPDATE from separate web requests. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(properties={
    "spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
    "spring.datasource.username=growlab",
    "spring.datasource.password=testpass",
    "server.servlet.session.cookie.secure=false"
})
class PostgresNotificationConcurrencyTest {
    @Autowired MockMvc mvc;
    @Autowired AccountNotificationRepository notifications;

    @Test
    void simultaneousSingleReadsKeepFirstTimestampAndOtherOwnerUntouched() throws Exception {
        var target=notifications.saveAndFlush(new AccountNotification(
            "pg-notification-race-owner","Chiến dịch đồng thời","Cần kiểm tra"));
        var foreign=notifications.saveAndFlush(new AccountNotification(
            "pg-notification-race-other","Hỗ trợ riêng tư","Không tiết lộ"));

        ExecutorService pool=Executors.newFixedThreadPool(3);
        CountDownLatch ready=new CountDownLatch(3), start=new CountDownLatch(1);
        var calls=new ArrayList<Future<Integer>>();
        try {
            for(int i=0;i<2;i++) {
                calls.add(pool.submit(()-> {
                    ready.countDown();start.await();
                    return mvc.perform(post("/notifications/"+target.getId()+"/read")
                        .with(user("pg-notification-race-owner")).with(csrf()))
                        .andReturn().getResponse().getStatus();
                }));
            }
            calls.add(pool.submit(()-> {
                ready.countDown();start.await();
                return mvc.perform(post("/notifications/"+foreign.getId()+"/read")
                    .with(user("pg-notification-race-owner")).with(csrf()))
                    .andReturn().getResponse().getStatus();
            }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(calls.get(0).get(20,TimeUnit.SECONDS)).isBetween(300,399);
            assertThat(calls.get(1).get(20,TimeUnit.SECONDS)).isBetween(300,399);
            assertThat(calls.get(2).get(20,TimeUnit.SECONDS)).isEqualTo(404);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
        var saved=notifications.findById(target.getId()).orElseThrow();
        assertThat(saved.getReadAt()).isNotNull();
        assertThat(notifications.findById(foreign.getId()).orElseThrow().getReadAt()).isNull();
        var first=saved.getReadAt();
        mvc.perform(post("/notifications/"+target.getId()+"/read")
            .with(user("pg-notification-race-owner")).with(csrf()));
        assertThat(notifications.findById(target.getId()).orElseThrow().getReadAt())
            .isEqualTo(first);
    }
}
