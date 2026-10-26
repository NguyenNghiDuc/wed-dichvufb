package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.GuardedAuthenticationProvider;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("prod")
@TestPropertySource(properties={"spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci",
 "spring.datasource.username=growlab","spring.datasource.password=testpass","server.servlet.session.cookie.secure=false"})
class PostgresAccountSessionSecurityTest {
 @Autowired UserRepository users; @Autowired PasswordEncoder encoder;
 @Autowired GuardedAuthenticationProvider provider; @Autowired JdbcTemplate jdbc;
 @Autowired javax.sql.DataSource dataSource;
 @Test void parallelFailuresCannotLoseCountsOrBypassAccountLock() throws Exception {
  User account=new User();account.setUsername("pg_lock_"+System.nanoTime());account.setPassword(encoder.encode("PgSessionPass123!"));users.saveAndFlush(account);
  var executor=Executors.newFixedThreadPool(5);var start=new CountDownLatch(1);
  try{
   var futures=IntStream.range(0,5).mapToObj(i->executor.submit(()->{
    start.await();
    assertThatThrownBy(()->provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(account.getUsername(),"incorrect")))
     .isInstanceOf(BadCredentialsException.class);return true;
   })).toList();
   start.countDown();for(var future:futures)assertThat(future.get(30,TimeUnit.SECONDS)).isTrue();
   var locked=users.findById(account.getId()).orElseThrow();assertThat(locked.getFailedLoginCount()).isEqualTo(5);
   assertThat(locked.getLoginLockedUntil()).isNotNull();
   assertThatThrownBy(()->provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(account.getUsername(),"PgSessionPass123!")))
    .isInstanceOf(LockedException.class);
  }finally{executor.shutdownNow();}
 }
 @Test void upgradeFromV11PreservesBcryptIdentityAndBusinessData() {
  String schema="session_upgrade_test";
  try{
   Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("11").load().migrate();
   String hash=encoder.encode("LegacyBcrypt123!");
   jdbc.update("insert into "+schema+".users(username,password,email,role,enabled,auth_version) values(?,?,?,?,?,?)",
     "legacy_upgrade",hash,"legacy@example.com","USER",true,0);
   jdbc.update("insert into "+schema+".tiktok_servers(name,country,description,unit_price_vnd,minimum_quantity,maximum_quantity,enabled,version) values('Legacy server','VN','Existing data',10,1,100,true,0)");
   Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
   assertThat(jdbc.queryForObject("select password from "+schema+".users where username='legacy_upgrade'",String.class)).isEqualTo(hash);
   assertThat(jdbc.queryForObject("select auth_version from "+schema+".users where username='legacy_upgrade'",Long.class)).isEqualTo(1);
   assertThat(jdbc.queryForObject("select failed_login_count from "+schema+".users where username='legacy_upgrade'",Integer.class)).isZero();
   assertThat(jdbc.queryForObject("select count(*) from "+schema+".tiktok_servers",Integer.class)).isEqualTo(1);
   assertThat(encoder.matches("LegacyBcrypt123!",hash)).isTrue();
  }finally{jdbc.execute("drop schema if exists "+schema+" cascade");}
 }
}
