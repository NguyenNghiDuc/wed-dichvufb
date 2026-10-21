package com.example.fbservice;
import com.example.fbservice.tiktok.*;
import com.example.fbservice.support.*;
import com.example.fbservice.content.*;
import com.example.fbservice.campaign.*;
import com.example.fbservice.notification.AccountNotificationRepository;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:unifiedworkflow;DB_CLOSE_DELAY=-1")
class UnifiedWorkflowIntegrationTest {
 @Autowired MockMvc mvc; @Autowired TiktokOrderingService ordering;
 @Autowired SupportTicketService support; @Autowired ContentDraftService content;
 @Autowired AccountNotificationRepository notices;
 @Test void publicPagesUseSharedThemeWithoutLeakingPrivilegedLinks() throws Exception {
  for(String path:new String[]{"/login","/register","/help"}){
   mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().string(containsString("class=\""+(path.equals("/help")?"account-page":"auth-page")+"\"")))
    .andExpect(content().string(containsString("/site.js")))
    .andExpect(content().string(not(containsString("/admin/users"))));
  }
  mvc.perform(get("/site.js")).andExpect(status().isOk());
 }
 @Test void roleAwareMenusKeepStaffAndAdminNavigationSeparate() throws Exception {
  mvc.perform(get("/notifications").with(user("ui_owner"))).andExpect(status().isOk())
   .andExpect(content().string(not(containsString("/admin/users"))));
  mvc.perform(get("/notifications").with(user("ui_staff").roles("STAFF"))).andExpect(status().isOk())
   .andExpect(content().string(containsString("/admin/support")))
   .andExpect(content().string(not(containsString("/admin/users"))));
  mvc.perform(get("/notifications").with(user("ui_admin").roles("ADMIN"))).andExpect(status().isOk())
   .andExpect(content().string(containsString("/admin/tiktok/servers")));
 }
 @Test void tiktokCsvIsOwnerScopedFilteredAndEscapesSpreadsheetFormulas() throws Exception {
  var server=ordering.saveServer(null,"VN CSV","VN","Manual",10,1,1000,true,0);
  var request=new TiktokOrderingService.Request("https://www.tiktok.com/@csv_demo","VN",server.getId(),100,"=1+1");
  var alice=ordering.create("csv_alice",request,0);ordering.create("csv_bob",request,0);
  mvc.perform(get("/tiktok/orders/export.csv").with(user("csv_alice")).param("owner","csv_bob"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
   .andExpect(content().string(containsString("csv_alice")))
   .andExpect(content().string(not(containsString("csv_bob"))))
   .andExpect(content().string(containsString("'=")));
  ordering.cancel("csv_alice",alice.getId());
  mvc.perform(get("/tiktok/orders/export.csv").with(user("csv_alice")).param("status","COMPLETED"))
   .andExpect(status().isOk()).andExpect(content().string(not(containsString("csv_alice"))));
  mvc.perform(get("/tiktok/orders/export.csv").with(user("csv_alice")).param("orderId","-1"))
   .andExpect(status().isBadRequest());
 }
 @Test void adminCsvRequiresAdminAndCustomerExportRequiresAuthentication() throws Exception {
  mvc.perform(get("/tiktok/orders/export.csv")).andExpect(status().is3xxRedirection());
  mvc.perform(get("/admin/tiktok/orders/export.csv").with(user("customer"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/tiktok/orders/export.csv").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/tiktok/orders/export.csv").with(user("operator").roles("ADMIN")))
   .andExpect(status().isOk()).andExpect(header().string("Content-Disposition",containsString("tiktok-orders.csv")));
 }
 @Test void invalidSupportFormPreservesSubmittedText() throws Exception {
  mvc.perform(post("/support").with(user("support_ui")).with(csrf()).param("subject","abc").param("description","Nội dung cần được giữ lại"))
   .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
   .andExpect(model().attribute("inputSubject","abc"))
   .andExpect(model().attribute("inputDescription","Nội dung cần được giữ lại"));
 }
 @Test void customerCanCloseOnlyOwnSupportWithCsrfAndRepeatedCloseIsSafe() throws Exception {
  var ticket=support.create("support_owner_ui","Cần trợ giúp","Mô tả yêu cầu hỗ trợ của tôi.");
  mvc.perform(post("/support/"+ticket.getId()+"/close").with(user("another")).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(post("/support/"+ticket.getId()+"/close").with(user("support_owner_ui"))).andExpect(status().isForbidden());
  for(int i=0;i<2;i++)mvc.perform(post("/support/"+ticket.getId()+"/close").with(user("support_owner_ui")).with(csrf()))
   .andExpect(status().is3xxRedirection());
  assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
  assertThat(notices.countByOwnerUsernameAndReadAtIsNull("support_owner_ui")).isEqualTo(2);
 }
 @Test void failedDraftEditKeepsInputsAndDoesNotModifyStoredDraft() throws Exception {
  var draft=content.save("draft_ui",SocialPlatform.TIKTOK,"Tiêu đề ban đầu","Nội dung ban đầu",null);
  mvc.perform(post("/content-calendar/"+draft.getId()+"/edit").with(user("draft_ui")).with(csrf())
    .param("platform","YOUTUBE").param("title","").param("body","Nội dung chưa lưu"))
   .andExpect(status().isOk()).andExpect(model().attribute("inputBody","Nội dung chưa lưu"))
   .andExpect(model().attribute("inputPlatform",SocialPlatform.YOUTUBE));
  assertThat(draft.getTitle()).isEqualTo("Tiêu đề ban đầu");
 }
 @Test void unicodePasswordOverBcryptLimitReturnsFormErrorInsteadOfServerFailure() throws Exception {
  mvc.perform(post("/register").with(csrf()).param("username","unicode_ui").param("email","user@example.com").param("password","ệ".repeat(30)))
   .andExpect(status().isOk()).andExpect(model().attributeExists("error"))
   .andExpect(model().attribute("registerUsername","unicode_ui"))
   .andExpect(content().string(not(containsString("ệ".repeat(30)))));
 }
}
