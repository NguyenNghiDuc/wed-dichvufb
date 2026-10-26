package com.example.fbservice;
import com.example.fbservice.campaign.*;
import com.example.fbservice.tiktok.*;
import com.example.fbservice.catalog.ServiceCatalogService;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:prioritycheckout;DB_CLOSE_DELAY=-1")
class PriorityCheckoutIntegrationTest {
 @Autowired MockMvc mvc;@Autowired CampaignOrderService campaigns;@Autowired CampaignOrderRepository orders;
 @Autowired TiktokOrderingService tiktok;@Autowired TiktokOrderRepository tiktokOrders;@Autowired ServiceCatalogService catalog;
 CampaignOrderRequest request(){return new CampaignOrderRequest(SocialPlatform.TIKTOK,CampaignServiceType.CONTENT_PLAN,"https://tiktok.com/@priority_demo","Kế hoạch nội dung kiểm thử đặt đơn theo bước.");}
 @Test void successfulWebPostRedirectsToOwnerScopedConfirmationAndRefreshIsReadOnly() throws Exception {
  var result=mvc.perform(post("/campaigns").with(user("priority_owner")).with(csrf()).param("platform","TIKTOK").param("serviceType","CONTENT_PLAN").param("targetUrl",request().targetUrl()).param("brief",request().brief()))
   .andExpect(redirectedUrlPattern("/campaigns/*/success")).andReturn();
  Long id=(Long)result.getFlashMap().get("createdOrderId");long count=orders.count();
  for(int i=0;i<2;i++)mvc.perform(get("/campaigns/"+id+"/success").with(user("priority_owner")))
   .andExpect(status().isOk()).andExpect(model().attribute("orderId",id)).andExpect(content().string(containsString("chưa phải xác nhận thanh toán")))
   .andExpect(content().string(containsString("/campaigns/"+id+"\"")));
  assertThat(orders.count()).isEqualTo(count);
 }
 @Test void confirmationNeverLeaksOrdersToOtherUsersEvenAdmins() throws Exception {
  var o=campaigns.create("priority_private",request());String url="/campaigns/"+o.getId()+"/success";
  mvc.perform(get(url).with(user("another"))).andExpect(status().isNotFound());
  mvc.perform(get(url).with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
  mvc.perform(get(url)).andExpect(status().is3xxRedirection());
  mvc.perform(get("/campaigns/999999/success").with(user("priority_private"))).andExpect(status().isNotFound());
 }
 @Test void legacyTiktokCreationIsGoneButArchivedConfirmationRemainsOwnerScoped() throws Exception {
  var server=tiktok.saveServer(null,"Priority test","VN","Manual",10,1,1000,true,0);
  mvc.perform(post("/tiktok/orders").with(user("priority_tiktok")).with(csrf()))
   .andExpect(status().isGone());
  assertThat(tiktokOrders.count()).isZero();
  var archived=tiktok.create("priority_tiktok",new TiktokOrderingService.Request("https://tiktok.com/@priority_demo","VN",server.getId(),100,""),0);
  String location="/tiktok/orders/"+archived.getId()+"/success";
  mvc.perform(get(location).with(user("priority_tiktok"))).andExpect(status().isOk()).andExpect(model().attribute("price",1000L));
  mvc.perform(get(location).with(user("another"))).andExpect(status().isNotFound());
 }
 @Test void quoteWithoutCouponReflectsServerCatalogAndDoesNotCreateAnOrder() throws Exception {
  catalog.update("admin",CampaignServiceType.CONTENT_PLAN,234567,true);long count=orders.count();
  mvc.perform(get("/campaigns/quote").with(user("priority_quote")).param("serviceType","CONTENT_PLAN"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.totalPriceVnd").value(234567)).andExpect(jsonPath("$.discountVnd").value(0));
  assertThat(orders.count()).isEqualTo(count);
  catalog.update("admin",CampaignServiceType.CONTENT_PLAN,234567,false);
  mvc.perform(get("/campaigns/quote").with(user("priority_quote")).param("serviceType","CONTENT_PLAN")).andExpect(status().isBadRequest());
 }
 @Test void wizardIsProgressiveAndInvalidSubmissionKeepsInputsWithoutCreatingOrder() throws Exception {
  mvc.perform(get("/campaigns/new").with(user("priority_form"))).andExpect(status().isOk())
   .andExpect(content().string(containsString("data-step=\"0\""))).andExpect(content().string(containsString("data-step=\"1\"")))
   .andExpect(content().string(containsString("data-step=\"2\""))).andExpect(content().string(containsString("id=\"checkout-confirm\"")));
  long count=orders.count();
  mvc.perform(post("/campaigns").with(user("priority_form")).with(csrf()).param("platform","TIKTOK").param("serviceType","CONTENT_PLAN").param("targetUrl","https://wrong.invalid").param("brief",request().brief()))
   .andExpect(status().isOk()).andExpect(model().attribute("targetUrl","https://wrong.invalid")).andExpect(model().attributeExists("error"));
  assertThat(orders.count()).isEqualTo(count);
 }
}
