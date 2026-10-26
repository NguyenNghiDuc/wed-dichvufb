package com.example.fbservice;
import com.example.fbservice.promotion.*;
import com.example.fbservice.campaign.*;
import com.example.fbservice.catalog.ServiceCatalogService;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:coupontest;DB_CLOSE_DELAY=-1")
class CouponIntegrationTest {
 @Autowired MockMvc mvc;@Autowired CouponService coupons;@Autowired CouponRepository items;
 @Autowired CampaignOrderService campaigns;@Autowired CampaignOrderRepository orders;
 @Autowired ServiceCatalogService catalog;@Autowired UserRepository users;@Autowired JdbcTemplate jdbc;
 String admin(String name){User u=new User();u.setUsername(name);u.setPassword("unused-fixture");u.setRole("ADMIN");users.saveAndFlush(u);return name;}
 Coupon make(String actor,String code,CouponType type,long amount,long cap,long min,int max,int per){var now=LocalDateTime.now(ZoneOffset.UTC);return coupons.create(actor,new CouponService.Definition(code,type,amount,cap,min,max,per,now.minusMinutes(1),now.plusDays(1)));}
 CampaignOrderRequest request(String code){return new CampaignOrderRequest(SocialPlatform.TIKTOK,CampaignServiceType.CONTENT_PLAN,"https://tiktok.com/@coupon_demo","Kế hoạch nội dung có sử dụng mã giảm giá.",code);}
 @Test void adminFormBindsUtcDatesNormalizesCodeAndRetainsInvalidInputs() throws Exception {
  String actor=admin("coupon_form_admin");
  mvc.perform(get("/admin/coupons/new").with(user(actor).roles("ADMIN"))).andExpect(status().isOk());
  var request=post("/admin/coupons").with(user(actor).roles("ADMIN")).with(csrf()).param("code","form25").param("type","PERCENT").param("amount","25").param("maximumDiscountVnd","10000").param("minimumOrderVnd","0").param("maximumUses","10").param("perUserLimit","1").param("startsAt","2026-01-01T00:00").param("endsAt","2030-01-01T00:00");
  mvc.perform(request).andExpect(status().is3xxRedirection());assertThat(items.findByCode("FORM25")).isPresent();
  mvc.perform(post("/admin/coupons").with(user(actor).roles("ADMIN")).with(csrf()).param("code","INVALID25").param("type","PERCENT").param("amount","101").param("maximumDiscountVnd","0").param("minimumOrderVnd","0").param("maximumUses","10").param("perUserLimit","1").param("startsAt","2026-01-01T00:00").param("endsAt","2030-01-01T00:00"))
   .andExpect(status().isOk()).andExpect(model().attributeExists("error")).andExpect(content().string(containsString("INVALID25")));
 }
 @Test void previewNeverConsumesAndWebAndApiIgnoreTamperedPrices() throws Exception {
  String actor=admin("coupon_price_admin");var coupon=make(actor,"PRICE10",CouponType.FIXED,10000,0,0,3,1);
  long base=catalog.quote(CampaignServiceType.CONTENT_PLAN);
  mvc.perform(get("/campaigns/quote").with(user("coupon_buyer")).param("serviceType","CONTENT_PLAN").param("couponCode","price10"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.originalPriceVnd").value(base)).andExpect(jsonPath("$.discountVnd").value(10000));
  assertThat(coupons.detail(coupon.getId()).getUsedCount()).isZero();
  var result=mvc.perform(post("/campaigns").with(user("coupon_buyer")).with(csrf()).param("platform","TIKTOK").param("serviceType","CONTENT_PLAN").param("targetUrl","https://tiktok.com/@coupon_demo").param("brief","Nội dung tư vấn cần thực hiện.").param("couponCode","price10").param("quotedPriceVnd","1").param("discountVnd","999999999"))
   .andExpect(redirectedUrlPattern("/campaigns/*/success")).andReturn();
  Long id=(Long)result.getFlashMap().get("createdOrderId");var order=orders.findById(id).orElseThrow();
  assertThat(order.getQuotedPriceVnd()).isEqualTo(base-10000);assertThat(order.getOriginalPriceVnd()).isEqualTo(base);assertThat(order.getCouponCode()).isEqualTo("PRICE10");
  mvc.perform(get("/campaigns/"+id).with(user("coupon_buyer"))).andExpect(status().isOk()).andExpect(content().string(containsString("PRICE10")));
  mvc.perform(post("/api/v1/campaign-orders").with(user("coupon_api_buyer")).with(csrf()).contentType("application/json").content("{\"platform\":\"TIKTOK\",\"serviceType\":\"CONTENT_PLAN\",\"targetUrl\":\"https://tiktok.com/@coupon_api\",\"brief\":\"Nội dung kiểm thử API giảm giá.\",\"couponCode\":\"PRICE10\",\"quotedPriceVnd\":1}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.quotedPriceVnd").value(base-10000));
  assertThat(coupons.detail(coupon.getId()).getUsedCount()).isEqualTo(2);
 }
 @Test void percentageRoundsDownCapsAtTotalAndOrderSnapshotSurvivesPriceChanges() {
  String actor=admin("coupon_math_admin");long before=catalog.quote(CampaignServiceType.CONTENT_PLAN);
  try{
   catalog.update(actor,CampaignServiceType.CONTENT_PLAN,10003,true);
   make(actor,"FLOOR07",CouponType.PERCENT,7,0,0,2,1);make(actor,"CAP500",CouponType.PERCENT,90,500,0,2,1);make(actor,"FULLFREE",CouponType.FIXED,50000000,0,0,2,1);
   var order=campaigns.create("coupon_math_buyer",request("FLOOR07"));assertThat(order.getDiscountVnd()).isEqualTo(700);assertThat(order.getQuotedPriceVnd()).isEqualTo(9303);
   assertThat(coupons.preview("coupon_math_buyer","CAP500",10003).discountVnd()).isEqualTo(500);
   assertThat(coupons.preview("coupon_math_buyer","FULLFREE",10003).totalPriceVnd()).isZero();
   catalog.update(actor,CampaignServiceType.CONTENT_PLAN,20000,true);
   var stored=orders.findById(order.getId()).orElseThrow();assertThat(stored.getOriginalPriceVnd()).isEqualTo(10003);assertThat(stored.getQuotedPriceVnd()).isEqualTo(9303);
  }finally{if(catalog.quote(CampaignServiceType.CONTENT_PLAN)!=before)catalog.update(actor,CampaignServiceType.CONTENT_PLAN,before,true);}
 }
 @Test void perUserAndGlobalLimitsHoldAndCancellationDoesNotRestoreUsage() {
  String actor=admin("coupon_limit_admin");var c=make(actor,"ONEONLY",CouponType.FIXED,1000,0,0,1,1);
  var order=campaigns.create("coupon_limit_buyer",request(c.getCode()));campaigns.cancelForOwner("coupon_limit_buyer",order.getId());
  assertThat(coupons.detail(c.getId()).getUsedCount()).isEqualTo(1);
  assertThatThrownBy(()->campaigns.create("another_buyer",request(c.getCode()))).isInstanceOf(IllegalArgumentException.class);
  var per=make(actor,"PERUSER",CouponType.FIXED,1000,0,0,3,1);campaigns.create("coupon_per_buyer",request(per.getCode()));
  assertThatThrownBy(()->campaigns.create("coupon_per_buyer",request(per.getCode()))).isInstanceOf(IllegalArgumentException.class);
  campaigns.create("coupon_per_other",request(per.getCode()));assertThat(coupons.detail(per.getId()).getUsedCount()).isEqualTo(2);
 }
 @Test void futureExpiredDisabledMinimumAndInvalidOrderNeverConsume() {
  String actor=admin("coupon_valid_admin");var now=LocalDateTime.now(ZoneOffset.UTC);
  var future=coupons.create(actor,new CouponService.Definition("FUTURE1",CouponType.FIXED,1000,0,0,1,1,now.plusDays(1),now.plusDays(2)));
  var past=coupons.create(actor,new CouponService.Definition("EXPIRED1",CouponType.FIXED,1000,0,0,1,1,now.minusDays(2),now.minusDays(1)));
  for(var c:new Coupon[]{future,past}){assertThatThrownBy(()->campaigns.create("coupon_invalid_buyer",request(c.getCode()))).isInstanceOf(IllegalArgumentException.class);assertThat(coupons.detail(c.getId()).getUsedCount()).isZero();}
  var min=make(actor,"MINHIGH",CouponType.FIXED,1000,0,50000000,1,1);assertThatThrownBy(()->campaigns.create("coupon_invalid_buyer",request(min.getCode()))).isInstanceOf(IllegalArgumentException.class);
  var off=make(actor,"TURNEDOFF",CouponType.FIXED,1000,0,0,1,1);coupons.setEnabled(actor,off.getId(),true,false);
  assertThatThrownBy(()->campaigns.create("coupon_invalid_buyer",request(off.getCode()))).isInstanceOf(IllegalArgumentException.class);
  var valid=make(actor,"BADORDER",CouponType.FIXED,1000,0,0,1,1);
  assertThatThrownBy(()->campaigns.create("coupon_invalid_buyer",new CampaignOrderRequest(SocialPlatform.TIKTOK,CampaignServiceType.CONTENT_PLAN,"https://evil.invalid","Nội dung có link không hợp lệ.",valid.getCode()))).isInstanceOf(IllegalArgumentException.class);
  assertThat(coupons.detail(valid.getId()).getUsedCount()).isZero();
 }
 @Test void permissionsCsrfToggleAuditAndFailedCheckoutRetention() throws Exception {
  String actor=admin("coupon_auth_admin");var c=make(actor,"AUTHCODE",CouponType.FIXED,1000,0,0,10,1);
  mvc.perform(get("/admin/coupons").with(user("customer"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/coupons/new").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
  mvc.perform(post("/admin/coupons/"+c.getId()+"/enabled").with(user(actor).roles("ADMIN")).param("expected","true").param("enabled","false")).andExpect(status().isForbidden());
  coupons.setEnabled(actor,c.getId(),true,false);assertThatThrownBy(()->coupons.setEnabled(actor,c.getId(),true,false)).isInstanceOf(IllegalArgumentException.class);assertThat(coupons.audit(c.getId())).hasSize(2);
  mvc.perform(get("/admin/coupons/"+c.getId()).with(user(actor).roles("ADMIN"))).andExpect(status().isOk());
  mvc.perform(post("/campaigns").with(user("coupon_error_buyer")).with(csrf()).param("platform","TIKTOK").param("serviceType","CONTENT_PLAN").param("targetUrl","https://tiktok.com/@coupon_demo").param("brief","Nội dung được giữ khi mã không hợp lệ.").param("couponCode","MISSINGCODE"))
   .andExpect(status().isOk()).andExpect(model().attribute("couponCode","MISSINGCODE")).andExpect(model().attribute("brief","Nội dung được giữ khi mã không hợp lệ."));
  mvc.perform(get("/campaigns/quote").with(user("coupon_error_buyer")).param("serviceType","CONTENT_PLAN").param("couponCode","<script>"))
   .andExpect(status().isBadRequest());
  mvc.perform(get("/admin/coupons").with(user(actor).roles("ADMIN")).param("page","-1")).andExpect(status().isBadRequest());
 }
}
