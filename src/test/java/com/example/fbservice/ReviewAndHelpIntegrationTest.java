package com.example.fbservice;
import com.example.fbservice.review.*;
import com.example.fbservice.campaign.*;
import com.example.fbservice.tiktok.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:reviewhelp;DB_CLOSE_DELAY=-1")
class ReviewAndHelpIntegrationTest {
 @Autowired MockMvc mvc; @Autowired CampaignOrderService campaigns;
 @Autowired TiktokOrderingService tiktok; @Autowired ServiceReviewService reviews;
 @Autowired ServiceReviewRepository stored;
 CampaignOrder campaign(String owner,boolean complete){
  var order=campaigns.create(owner,new CampaignOrderRequest(SocialPlatform.TIKTOK,CampaignServiceType.CONTENT_PLAN,"https://tiktok.com/@review_demo","Kế hoạch nội dung cho khách hàng."));
  if(complete)for(var s:new CampaignStatus[]{CampaignStatus.APPROVED,CampaignStatus.IN_PROGRESS,CampaignStatus.COMPLETED})campaigns.updateStatusForAdmin(order.getId(),s,"admin");
  return order;
 }
 @Test void reviewRequiresCompletedOwnedOrderAndCsrf() throws Exception {
  var pending=campaign("review_owner",false);var complete=campaign("review_owner",true);
  String path="/reviews/CAMPAIGN/"+complete.getId();
  mvc.perform(get(path).with(user("other"))).andExpect(status().isNotFound());
  mvc.perform(get("/reviews/CAMPAIGN/"+pending.getId()).with(user("review_owner"))).andExpect(status().isBadRequest());
  mvc.perform(post(path).with(user("review_owner")).param("rating","5").param("comment","Dịch vụ rất tốt và rõ ràng.")).andExpect(status().isForbidden());
  mvc.perform(post(path).with(user("other")).with(csrf()).param("rating","5").param("comment","Dịch vụ rất tốt và rõ ràng.")).andExpect(status().isNotFound());
  mvc.perform(get(path)).andExpect(status().is3xxRedirection());assertThat(stored.count()).isZero();
 }
 @Test void invalidReviewPreservesInputAndDuplicateCannotOverwriteOriginal() throws Exception {
  var order=campaign("review_form",true);String path="/reviews/CAMPAIGN/"+order.getId();
  mvc.perform(post(path).with(user("review_form")).with(csrf()).param("rating","6").param("comment","Nội dung cần giữ nguyên"))
   .andExpect(status().isOk()).andExpect(model().attribute("inputComment","Nội dung cần giữ nguyên")).andExpect(model().attributeExists("error"));
  mvc.perform(post(path).with(user("review_form")).with(csrf()).param("rating","4").param("comment","<script>alert('xss')</script>"))
   .andExpect(redirectedUrl(path));
  mvc.perform(get(path).with(user("review_form"))).andExpect(status().isOk())
   .andExpect(content().string(containsString("&lt;script&gt;"))).andExpect(content().string(not(containsString("<script>alert"))));
  mvc.perform(post(path).with(user("review_form")).with(csrf()).param("rating","1").param("comment","Không được ghi đè nhận xét cũ"))
   .andExpect(redirectedUrl(path)).andExpect(flash().attributeExists("error"));assertThat(stored.count()).isEqualTo(1);assertThat(stored.findAll().get(0).getRating()).isEqualTo(4);
 }
 @Test void tiktokCompletedReviewAndAdminFiltersRender() throws Exception {
  var server=tiktok.saveServer(null,"Review test","VN","Manual test",10,1,1000,true,0);
  var order=tiktok.create("review_tiktok",new TiktokOrderingService.Request("https://www.tiktok.com/@review_demo","VN",server.getId(),10,""),0);
  for(var s:new CampaignStatus[]{CampaignStatus.APPROVED,CampaignStatus.IN_PROGRESS,CampaignStatus.COMPLETED})tiktok.updateStatus("admin",order.getId(),s);
  reviews.create("review_tiktok",ReviewKind.TIKTOK,order.getId(),3,"Nhân viên phản hồi tốt nhưng còn chậm.");
  mvc.perform(get("/tiktok/orders/"+order.getId()).with(user("review_tiktok"))).andExpect(status().isOk()).andExpect(content().string(containsString("/reviews/TIKTOK/"+order.getId())));
  mvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN")).param("rating","3").param("kind","TIKTOK"))
   .andExpect(status().isOk()).andExpect(content().string(containsString("review_tiktok")));
  assertThat(reviews.admin(0,5,ReviewKind.CAMPAIGN).isEmpty()).isTrue();
  mvc.perform(get("/admin/reviews").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN")).param("rating","0")).andExpect(status().isBadRequest());
 }
 @Test void faqSearchAcceptsVietnameseWithoutAccentsAndCombinesCategory() throws Exception {
  mvc.perform(get("/help").param("q","MAT KHAU").param("category","account"))
   .andExpect(status().isOk()).andExpect(content().string(containsString("Đổi hoặc quên mật khẩu")))
   .andExpect(content().string(not(containsString("Đặt chiến dịch như thế nào?"))));
  mvc.perform(get("/help").param("q","huy don")).andExpect(status().isOk()).andExpect(content().string(containsString("Hủy đơn được không?")));
  mvc.perform(get("/help").param("q","<script>alert(1)</script>"))
   .andExpect(status().isOk()).andExpect(content().string(containsString("Chưa tìm thấy hướng dẫn phù hợp")))
   .andExpect(content().string(not(containsString("<script>alert"))));
  mvc.perform(get("/help").param("category","invalid")).andExpect(status().isBadRequest());
  mvc.perform(get("/help").param("q","x".repeat(121))).andExpect(status().isBadRequest());
 }
}
