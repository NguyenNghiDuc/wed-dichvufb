package com.example.fbservice;
import com.example.fbservice.broadcast.*;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.notification.AccountNotificationRepository;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:broadcasttest;DB_CLOSE_DELAY=-1")
class BroadcastIntegrationTest {
 @Autowired MockMvc mvc;@Autowired BroadcastService service;@Autowired UserRepository users;
 @Autowired JdbcTemplate jdbc;@Autowired AccountNotificationRepository notices;
 User account(String name,String role,boolean enabled){var u=new User();u.setUsername(name);u.setPassword("not-a-login-fixture");u.setRole(role);u.setEnabled(enabled);return users.saveAndFlush(u);}
 long delivered(Long id){return jdbc.queryForObject("select count(*) from account_notifications where broadcast_id=?",Long.class,id);}
 @Test void draftDoesNotSendAndAudienceExcludesDisabledAndOtherRolesAndFutureSignups() throws Exception {
  var admin=account("bc_admin_scope","ADMIN",true);var customer=account("bc_user_scope","USER",true);
  account("bc_disabled","USER",false);account("bc_staff","STAFF",true);
  var draft=service.create(admin.getUsername(),"Thông báo cập nhật","Nội dung chung dành cho khách hàng.",BroadcastAudience.USER);
  assertThat(delivered(draft.getId())).isZero();int count=(int)service.recipients(BroadcastAudience.USER);
  service.send(admin.getUsername(),draft.getId(),count);service.send(admin.getUsername(),draft.getId(),count);
  assertThat(delivered(draft.getId())).isEqualTo(count);
  assertThat(notices.countByOwnerUsernameAndReadAtIsNull(customer.getUsername())).isEqualTo(1);
  assertThat(notices.countByOwnerUsernameAndReadAtIsNull("bc_disabled")).isZero();
  assertThat(notices.countByOwnerUsernameAndReadAtIsNull("bc_staff")).isZero();
  account("bc_future","USER",true);assertThat(notices.countByOwnerUsernameAndReadAtIsNull("bc_future")).isZero();
  mvc.perform(get("/notifications").with(user(customer.getUsername()))).andExpect(status().isOk()).andExpect(content().string(containsString("Thông báo hệ thống")));
  mvc.perform(post("/notifications/read-all").with(user(customer.getUsername())).with(csrf())).andExpect(redirectedUrl("/notifications"));
  assertThat(notices.countByOwnerUsernameAndReadAtIsNull(customer.getUsername())).isZero();
  assertThat(service.history(draft.getId())).hasSize(2);
 }
 @Test void permissionsCsrfAndPersistedAdminStatusAreChecked() throws Exception {
  var actor=account("bc_admin_auth","ADMIN",true);account("bc_false_admin","USER",true);
  var draft=service.create(actor.getUsername(),"Thông báo quyền hạn","Nội dung kiểm tra quyền gửi thông báo.",BroadcastAudience.ADMIN);
  String path="/admin/broadcasts/"+draft.getId();
  mvc.perform(get(path).with(user("customer"))).andExpect(status().isForbidden());
  mvc.perform(get(path).with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
  mvc.perform(post(path+"/send").with(user(actor.getUsername()).roles("ADMIN")).param("confirmedRecipients","1")).andExpect(status().isForbidden());
  mvc.perform(post(path+"/send").with(user("bc_false_admin").roles("ADMIN")).with(csrf()).param("confirmedRecipients","1")).andExpect(status().isForbidden());
  assertThat(delivered(draft.getId())).isZero();
 }
 @Test void changedAudienceAndCancelledDraftNeverSend() {
  var actor=account("bc_admin_cancel","ADMIN",true);
  var draft=service.create(actor.getUsername(),"Thông báo hủy thử","Nội dung kiểm tra bản nháp bị hủy.",BroadcastAudience.ADMIN);
  int count=(int)service.recipients(BroadcastAudience.ADMIN);account("bc_admin_new","ADMIN",true);
  assertThatThrownBy(()->service.send(actor.getUsername(),draft.getId(),count)).isInstanceOf(IllegalArgumentException.class);
  assertThat(delivered(draft.getId())).isZero();assertThat(service.detail(draft.getId()).getStatus()).isEqualTo(BroadcastStatus.DRAFT);
  service.cancel(actor.getUsername(),draft.getId());service.cancel(actor.getUsername(),draft.getId());
  assertThatThrownBy(()->service.send(actor.getUsername(),draft.getId(),count+1)).isInstanceOf(IllegalArgumentException.class);
  assertThat(service.history(draft.getId())).hasSize(2);assertThat(delivered(draft.getId())).isZero();
 }
 @Test void invalidFormPreservesInputsAndPreviewEscapesHtml() throws Exception {
  var actor=account("bc_admin_form","ADMIN",true);
  mvc.perform(post("/admin/broadcasts").with(user(actor.getUsername()).roles("ADMIN")).with(csrf())
    .param("title","abc").param("message","Nội dung phải được giữ lại").param("audience","USER"))
   .andExpect(status().isOk()).andExpect(model().attribute("inputMessage","Nội dung phải được giữ lại"))
   .andExpect(model().attributeExists("error"));
  var draft=service.create(actor.getUsername(),"Thông báo <script>","<script>alert('xss')</script>",BroadcastAudience.ALL);
  mvc.perform(get("/admin/broadcasts/"+draft.getId()).with(user(actor.getUsername()).roles("ADMIN")))
   .andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;")))
   .andExpect(content().string(not(containsString("<script>alert"))))
   .andExpect(model().attributeExists("recipientCount"));
  mvc.perform(get("/admin/broadcasts").with(user(actor.getUsername()).roles("ADMIN")).param("page","-1")).andExpect(status().isBadRequest());
  mvc.perform(get("/admin/broadcasts").with(user(actor.getUsername()).roles("ADMIN")).param("status","INVALID")).andExpect(status().isBadRequest());
 }
 @Test void oversizedAudienceIsRejectedWithoutAnyDelivery() {
  var actor=account("bc_admin_limit","ADMIN",true);
  jdbc.update("insert into users(username,password,role,enabled,auth_version,failed_login_count) select concat('bc_cap_',x),'unused-fixture','STAFF',true,0,0 from system_range(1,5001)");
  try{
   var draft=service.create(actor.getUsername(),"Thông báo giới hạn","Nội dung kiểm tra nhóm nhận quá lớn.",BroadcastAudience.STAFF);
   int count=(int)service.recipients(BroadcastAudience.STAFF);
   assertThatThrownBy(()->service.send(actor.getUsername(),draft.getId(),count)).isInstanceOf(IllegalArgumentException.class);
   assertThat(delivered(draft.getId())).isZero();assertThat(service.detail(draft.getId()).getStatus()).isEqualTo(BroadcastStatus.DRAFT);
  }finally{jdbc.update("delete from users where username like 'bc_cap_%'");}
 }
 @Test void listPaginationRetainsStatusAndSentAnnouncementCannotBeCancelled() throws Exception {
  var actor=account("bc_admin_pages","ADMIN",true);
  for(int i=0;i<21;i++)service.create(actor.getUsername(),"Thông báo trang "+i,"Nội dung phục vụ kiểm thử phân trang.",BroadcastAudience.ADMIN);
  mvc.perform(get("/admin/broadcasts").with(user(actor.getUsername()).roles("ADMIN")).param("status","DRAFT"))
   .andExpect(status().isOk()).andExpect(content().string(containsString("page=1&amp;status=DRAFT")));
  var draft=service.create(actor.getUsername(),"Thông báo đã gửi","Nội dung kiểm tra không thu hồi đã gửi.",BroadcastAudience.ADMIN);
  service.send(actor.getUsername(),draft.getId(),(int)service.recipients(BroadcastAudience.ADMIN));
  assertThatThrownBy(()->service.cancel(actor.getUsername(),draft.getId())).isInstanceOf(IllegalArgumentException.class);
  assertThat(service.detail(draft.getId()).getStatus()).isEqualTo(BroadcastStatus.SENT);
 }
}
