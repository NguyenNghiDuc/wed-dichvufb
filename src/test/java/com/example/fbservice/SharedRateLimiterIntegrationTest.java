package com.example.fbservice;

import com.example.fbservice.config.TrafficLimiter;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.service.DepositService;
import com.example.fbservice.model.DepositRequest;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:sharedlimits;DB_CLOSE_DELAY=-1")
class SharedRateLimiterIntegrationTest {
 @Autowired DataSource source; @Autowired PlatformTransactionManager transactions;
 @Autowired DepositService deposits; @Autowired UserRepository users;
 @Test void countersAreSharedAtomicAndSurviveBusinessRollback() throws Exception {
  var first=new TrafficLimiter(source,transactions,Clock.systemUTC());
  var second=new TrafficLimiter(source,transactions,Clock.systemUTC());
  String key="shared-"+System.nanoTime();var pool=Executors.newFixedThreadPool(8);var start=new CountDownLatch(1);
  try{
   var jobs=java.util.stream.IntStream.range(0,24).mapToObj(i->pool.submit(()->{
    start.await();try{(i%2==0?first:second).check(key,7,Duration.ofMinutes(1));return 1;}
    catch(ResponseStatusException e){assertThat(e.getStatusCode().value()).isEqualTo(429);return 0;}
   })).toList();start.countDown();int accepted=0;for(var job:jobs)accepted+=job.get(30,TimeUnit.SECONDS);
   assertThat(accepted).isEqualTo(7);
  }finally{pool.shutdownNow();}
  String rollback="rollback-"+System.nanoTime();
  assertThatThrownBy(()->new TransactionTemplate(transactions).executeWithoutResult(s->{first.check(rollback,1,Duration.ofMinutes(1));throw new IllegalStateException("rollback");})).isInstanceOf(IllegalStateException.class);
  assertThatThrownBy(()->second.check(rollback,1,Duration.ofMinutes(1))).isInstanceOf(ResponseStatusException.class);
 }
 @Test void expiryBelongsToEachBucketRatherThanTheCallersPeriod(){
  AtomicLong time=new AtomicLong(100000);
  Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return Instant.ofEpochMilli(time.get());}};
  var limiter=new TrafficLimiter(source,transactions,clock);String key="expiry-"+System.nanoTime();
  limiter.check(key,1,Duration.ofMinutes(15));time.addAndGet(Duration.ofMinutes(1).toMillis());
  assertThatThrownBy(()->limiter.check(key,1,Duration.ofSeconds(1))).isInstanceOf(ResponseStatusException.class);
  time.addAndGet(Duration.ofMinutes(15).toMillis());limiter.check(key,1,Duration.ofMinutes(15));
 }
 @Test void pendingDepositCapRemainsAtomicForConcurrentRequests() throws Exception {
  String name="pending_"+System.nanoTime();User user=new User();user.setUsername(name);user.setPassword("test-fixture");users.saveAndFlush(user);
  for(int i=0;i<24;i++)deposits.save(deposit(),name);
  var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  try{
   var tasks=java.util.stream.IntStream.range(0,2).mapToObj(i->pool.submit(()->{gate.await();try{deposits.save(deposit(),name);return 1;}catch(ResponseStatusException e){assertThat(e.getStatusCode().value()).isEqualTo(429);return 0;}})).toList();
   gate.countDown();int accepted=0;for(var task:tasks)accepted+=task.get(20,TimeUnit.SECONDS);assertThat(accepted).isEqualTo(1);
  }finally{pool.shutdownNow();}
 }
 private DepositRequest deposit(){var d=new DepositRequest();d.setAmount(10000);return d;}
}
