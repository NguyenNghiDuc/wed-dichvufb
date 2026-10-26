package com.example.fbservice;
import com.example.fbservice.promotion.*;
import com.example.fbservice.campaign.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.time.*;
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
class PostgresCouponAtomicityTest {
 @Autowired CouponService coupons;@Autowired CampaignOrderService campaigns;@Autowired UserRepository users;@Autowired JdbcTemplate jdbc;
 Coupon make(String code){var u=new User();u.setUsername("pgcp_"+System.nanoTime());u.setPassword("unused-fixture");u.setRole("ADMIN");users.saveAndFlush(u);var now=LocalDateTime.now(ZoneOffset.UTC);return coupons.create(u.getUsername(),new CouponService.Definition(code,CouponType.FIXED,1000,0,0,1,1,now.minusMinutes(1),now.plusDays(1)));}
 CampaignOrderRequest request(String code){return new CampaignOrderRequest(SocialPlatform.TIKTOK,CampaignServiceType.CONTENT_PLAN,"https://tiktok.com/@coupon_pg","Nội dung kiểm thử giao dịch mã giảm giá.",code);}
 @Test void onlyOneConcurrentOrderCanTakeTheLastUse() throws Exception {
  var c=make("LAST"+System.nanoTime());var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  try{Callable<Boolean> a=()->{gate.await();try{campaigns.create("pg_coupon_a",request(c.getCode()));return true;}catch(IllegalArgumentException e){return false;}};
   Callable<Boolean> b=()->{gate.await();try{campaigns.create("pg_coupon_b",request(c.getCode()));return true;}catch(IllegalArgumentException e){return false;}};
   var first=pool.submit(a);var second=pool.submit(b);gate.countDown();assertThat(java.util.List.of(first.get(30,TimeUnit.SECONDS),second.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
   assertThat(coupons.detail(c.getId()).getUsedCount()).isEqualTo(1);assertThat(coupons.uses(c.getId(),0).getTotalElements()).isEqualTo(1);
  }finally{pool.shutdownNow();}
 }
 @Test void failedUsageInsertRollsBackOrderAndCouponCounterThenRetrySucceeds() {
  var c=make("ROLL"+System.nanoTime());String owner="pg_coupon_rollback_"+c.getId();
  jdbc.execute("create function fail_coupon_usage() returns trigger language plpgsql as $$ begin if NEW.coupon_id="+c.getId()+" then raise exception 'intentional CI coupon failure'; end if; return NEW; end $$");
  jdbc.execute("create trigger fail_coupon_usage before insert on coupon_uses for each row execute function fail_coupon_usage()");
  try{assertThatThrownBy(()->campaigns.create(owner,request(c.getCode()))).isInstanceOf(RuntimeException.class);
   assertThat(coupons.detail(c.getId()).getUsedCount()).isZero();assertThat(coupons.uses(c.getId(),0).getTotalElements()).isZero();assertThat(campaigns.pageMine(owner,0,20).getTotalElements()).isZero();
  }finally{jdbc.execute("drop trigger if exists fail_coupon_usage on coupon_uses");jdbc.execute("drop function if exists fail_coupon_usage()");}
  campaigns.create(owner,request(c.getCode()));assertThat(coupons.detail(c.getId()).getUsedCount()).isEqualTo(1);assertThat(campaigns.pageMine(owner,0,20).getTotalElements()).isEqualTo(1);
 }
}
