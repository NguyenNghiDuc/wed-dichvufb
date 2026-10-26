package com.example.fbservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest @ActiveProfiles("prod")
@TestPropertySource(properties={"spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
 "spring.datasource.username=growlab","spring.datasource.password=testpass","server.servlet.session.cookie.secure=false"})
class PostgresSharedRateLimiterIntegrationTest extends SharedRateLimiterIntegrationTest {}
