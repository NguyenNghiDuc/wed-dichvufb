package com.example.fbservice;
import com.example.fbservice.broadcast.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @ActiveProfiles("prod")
@TestPropertySource(properties={"spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci","spring.datasource.username=growlab","spring.datasource.password=testpass","server.servlet.session.cookie.secure=false"})
class PostgresBroadcastDeliveryTest {
 @Autowired BroadcastService service;@Autowired UserRepository users;@Autowired JdbcTemplate jdbc;
 User admin(){var u=new User();u.setUsername("pg_bc_"+System.nanoTime());u.setPassword("unused-fixture");u.setRole("ADMIN");return users.saveAndFlush(u);}
 long rows(Long id){return jdbc.queryForObject("select count(*) from account_notifications where broadcast_id=?",Long.class,id);}
 @Test void simultaneousSendsByDifferentAdminsDeliverOnce() throws Exception {
  var firstAdmin=admin();var secondAdmin=admin();var b=service.create(firstAdmin.getUsername(),"Gửi đồng thời","Nội dung chỉ gửi một lần mỗi người nhận.",BroadcastAudience.ADMIN);
  int count=(int)service.recipients(BroadcastAudience.ADMIN);var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try{var first=pool.submit(()->{gate.await();return service.send(firstAdmin.getUsername(),b.getId(),count);});var second=pool.submit(()->{gate.await();return service.send(secondAdmin.getUsername(),b.getId(),count);});gate.countDown();
   assertThat(first.get(30,TimeUnit.SECONDS).getStatus()).isEqualTo(BroadcastStatus.SENT);assertThat(second.get(30,TimeUnit.SECONDS).getStatus()).isEqualTo(BroadcastStatus.SENT);
   assertThat(rows(b.getId())).isEqualTo(count);assertThat(service.history(b.getId())).hasSize(2);
  }finally{pool.shutdownNow();}
 }
 @Test void partialInsertMismatchRollsBackAllNotificationsAndAllowsRetry() {
  var actor=admin();var b=service.create(actor.getUsername(),"Kiểm tra rollback","Không để lại thông báo gửi dở khi gặp lỗi.",BroadcastAudience.ADMIN);
  int count=(int)service.recipients(BroadcastAudience.ADMIN);
  jdbc.execute("create function bc_skip_recipient() returns trigger language plpgsql as $$ begin if NEW.broadcast_id="+b.getId()+" and NEW.owner_username='"+actor.getUsername()+"' then return null; end if; return NEW; end $$");
  jdbc.execute("create trigger bc_skip before insert on account_notifications for each row execute function bc_skip_recipient()");
  try{assertThatThrownBy(()->service.send(actor.getUsername(),b.getId(),count)).isInstanceOf(IllegalArgumentException.class);
   assertThat(rows(b.getId())).isZero();assertThat(service.detail(b.getId()).getStatus()).isEqualTo(BroadcastStatus.DRAFT);assertThat(service.history(b.getId())).hasSize(1);
  }finally{jdbc.execute("drop trigger if exists bc_skip on account_notifications");jdbc.execute("drop function if exists bc_skip_recipient()");}
  service.send(actor.getUsername(),b.getId(),count);assertThat(rows(b.getId())).isEqualTo(count);
 }
}
