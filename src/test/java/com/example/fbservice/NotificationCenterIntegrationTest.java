package com.example.fbservice;
import com.example.fbservice.notification.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional @WithMockUser(username="notice_center_owner")
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:noticecenter;DB_CLOSE_DELAY=-1")
class NotificationCenterIntegrationTest {
 @Autowired MockMvc mvc; @Autowired AccountNotificationRepository items;
 @Test void paginationAndUnreadFilterRenderOnlyOwnersNotifications() throws Exception {
  for(int i=0;i<21;i++)items.save(new AccountNotification("notice_center_owner","Thông báo "+i,"Nội dung"));
  items.save(new AccountNotification("someone_else","Không được xem","Riêng tư"));
  mvc.perform(get("/notifications")).andExpect(status().isOk()).andExpect(model().attribute("unread",21L));
  mvc.perform(get("/notifications").param("page","1").param("unreadOnly","true"))
   .andExpect(status().isOk()).andExpect(model().attribute("unreadOnly",true));
  mvc.perform(get("/notifications").param("page","-1")).andExpect(status().isBadRequest());
 }
 @Test void markAllReadIsCsrfProtectedIdempotentAndOwnerScoped() throws Exception {
  var mine=items.save(new AccountNotification("notice_center_owner","Mine","Message"));
  var other=items.save(new AccountNotification("someone_else","Other","Private"));
  mvc.perform(post("/notifications/read-all")).andExpect(status().isForbidden());
  mvc.perform(post("/notifications/read-all").with(csrf())).andExpect(redirectedUrl("/notifications"));
  assertThat(items.findById(mine.getId()).orElseThrow().getReadAt()).isNotNull();
  assertThat(items.findById(other.getId()).orElseThrow().getReadAt()).isNull();
  mvc.perform(post("/notifications/read-all").with(csrf())).andExpect(redirectedUrl("/notifications"));
  assertThat(items.countByOwnerUsernameAndReadAtIsNull("notice_center_owner")).isZero();
 }
}
