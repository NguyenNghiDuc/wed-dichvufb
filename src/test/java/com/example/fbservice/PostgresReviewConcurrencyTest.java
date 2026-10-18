package com.example.fbservice;
import com.example.fbservice.campaign.*;
import com.example.fbservice.review.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @ActiveProfiles("prod")
@TestPropertySource(properties={"spring.datasource.url=jdbc:postgresql://localhost:5432/growlab_ci","spring.datasource.username=growlab","spring.datasource.password=testpass","server.servlet.session.cookie.secure=false"})
class PostgresReviewConcurrencyTest {
 @Autowired CampaignOrderService campaigns; @Autowired ServiceReviewService reviews;
 @Test void simultaneousSubmissionsProduceExactlyOneReview() throws Exception {
  String owner="pg_review_"+System.nanoTime();var o=campaigns.create(owner,new CampaignOrderRequest(SocialPlatform.YOUTUBE,CampaignServiceType.CONTENT_PLAN,"https://youtube.com/@review_demo","Kế hoạch nội dung kiểm thử đồng thời."));
  for(var s:new CampaignStatus[]{CampaignStatus.APPROVED,CampaignStatus.IN_PROGRESS,CampaignStatus.COMPLETED})campaigns.updateStatusForAdmin(o.getId(),s,"admin");
  var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  Callable<Integer> submit=()->{gate.await();try{reviews.create(owner,ReviewKind.CAMPAIGN,o.getId(),5,"Nhân viên hướng dẫn rất rõ ràng.");return 201;}catch(ResponseStatusException e){return e.getStatusCode().value();}};
  try{var first=pool.submit(submit);var second=pool.submit(submit);gate.countDown();assertThat(java.util.List.of(first.get(30,TimeUnit.SECONDS),second.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
   assertThat(reviews.mine(owner,ReviewKind.CAMPAIGN,o.getId())).isPresent();
  }finally{pool.shutdownNow();}
 }
}
