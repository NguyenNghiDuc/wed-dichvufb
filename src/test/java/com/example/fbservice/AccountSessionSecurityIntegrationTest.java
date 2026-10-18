package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.*;
import com.example.fbservice.notification.AccountNotificationRepository;
import com.example.fbservice.service.AccountSettingsService;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:sessionsecurity;DB_CLOSE_DELAY=-1")
class AccountSessionSecurityIntegrationTest {
 @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired PasswordEncoder encoder;
 @Autowired AccountSessionService sessions; @Autowired AccountSecurityEventRepository events;
 @Autowired AccountNotificationRepository notices; @Autowired AccountSettingsService settings;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Autowired com.example.fbservice.admin.AdminUserService admin;
 static final AtomicInteger address=new AtomicInteger(1);
 User account(String name){User u=new User();u.setUsername(name);u.setPassword(encoder.encode("SessionPass123!"));return users.saveAndFlush(u);}
 MockHttpSession login(String name,String device) throws Exception {
  return (MockHttpSession)mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr("198.51.100."+address.getAndIncrement());return r;})
    .header("User-Agent",device).param("username",name).param("password","SessionPass123!"))
   .andExpect(redirectedUrl("/campaigns")).andReturn().getRequest().getSession(false);
 }
 @Test void sessionsStoreOnlyHashAndRotateAnonymousIdentityAndIgnoreForwardedIp() throws Exception {
  var u=account("device_metadata");var anonymous=new MockHttpSession();String oldId=anonymous.getId();
  var result=mvc.perform(post("/login").session(anonymous).with(csrf()).with(r->{r.setRemoteAddr("192.0.2.10");return r;})
    .header("X-Forwarded-For","203.0.113.99").header("User-Agent","<script>alert(1)</script> Browser")
    .param("username",u.getUsername()).param("password","SessionPass123!"))
   .andExpect(redirectedUrl("/campaigns")).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);
  assertThat(session.getId()).isNotEqualTo(oldId);
  assertThat(jdbc.queryForObject("select session_hash from account_sessions where user_id=?",String.class,u.getId()))
   .isEqualTo(ClientMetadata.hash(session.getId())).isNotEqualTo(session.getId());
  assertThat(sessions.list(u,session.getId())).singleElement().satisfies(s->{assertThat(s.current()).isTrue();assertThat(s.ipAddress()).isEqualTo("192.0.2.10");});
  mvc.perform(get("/profile/security").session(session)).andExpect(status().isOk())
   .andExpect(content().string(containsString("&lt;script&gt;")))
   .andExpect(content().string(not(containsString("<script>alert"))))
   .andExpect(content().string(not(containsString(session.getId()))))
   .andExpect(content().string(not(containsString(ClientMetadata.hash(session.getId())))));
 }
 @Test void loggingInAgainInTheSameBrowserDoesNotLeaveAGhostSession() throws Exception {
  var u=account("device_relogin");var first=login(u.getUsername(),"Relogin browser");String oldId=first.getId();
  var next=mvc.perform(post("/login").session(first).with(csrf()).param("username",u.getUsername()).param("password","SessionPass123!"))
   .andExpect(redirectedUrl("/campaigns")).andReturn().getRequest().getSession(false);
  assertThat(next.getId()).isNotEqualTo(oldId);assertThat(sessions.list(u,next.getId())).hasSize(1);
 }
 @Test void revokeOneSessionRequiresPasswordCsrfAndOwnershipAndLeavesOtherSessionUsable() throws Exception {
  var alice=account("device_alice");var bob=account("device_bob");
  var first=login(alice.getUsername(),"First browser");var second=login(alice.getUsername(),"Second browser");
  var b=login(bob.getUsername(),"Bob browser");
  Long firstId=sessions.list(alice,first.getId()).stream().filter(AccountSessionService.SessionView::current).findFirst().orElseThrow().id();
  String path="/profile/security/sessions/"+firstId+"/revoke";
  mvc.perform(post(path).session(second).param("currentPassword","SessionPass123!")).andExpect(status().isForbidden());
  mvc.perform(post(path).session(b).with(csrf()).param("currentPassword","SessionPass123!")).andExpect(status().isNotFound());
  mvc.perform(post(path).session(second).with(csrf()).param("currentPassword","wrong"))
   .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
  assertThat(sessions.list(alice,null)).hasSize(2);
  mvc.perform(post(path).session(second).with(csrf()).param("currentPassword","SessionPass123!"))
   .andExpect(redirectedUrl("/profile/security")).andExpect(flash().attributeExists("success"));
  mvc.perform(get("/profile").session(first)).andExpect(status().isForbidden());
  mvc.perform(get("/profile").session(second)).andExpect(status().isOk());
  assertThat(sessions.history(alice.getId(),0).getContent()).extracting(AccountSecurityEvent::getType).contains(SecurityEventType.SESSION_REVOKED);
 }
 @Test void revokingCurrentSessionAndLoggingOutRemoveItFromActiveList() throws Exception {
  var u=account("device_current");var s=login(u.getUsername(),"Current browser");
  Long id=sessions.list(u,s.getId()).get(0).id();
  mvc.perform(post("/profile/security/sessions/"+id+"/revoke").session(s).with(csrf()).param("currentPassword","SessionPass123!"))
   .andExpect(redirectedUrl("/login?sessionClosed"));
  assertThat(s.isInvalid()).isTrue();assertThat(sessions.list(u,null)).isEmpty();
  var next=login(u.getUsername(),"Logout browser");
  mvc.perform(post("/logout").session(next).with(csrf())).andExpect(redirectedUrl("/"));
  assertThat(sessions.list(u,null)).isEmpty();
  assertThat(sessions.history(u.getId(),0).getContent()).extracting(AccountSecurityEvent::getType).contains(SecurityEventType.LOGOUT);
 }
 @Test void fiveFailuresAcrossIpsPersistAndLockDoesNotExtendAndEventuallyExpires() throws Exception {
  var u=account("device_lockout");var active=login(u.getUsername(),"Known browser");
  for(int i=0;i<5;i++)mvc.perform(post("/login").with(csrf()).with(r->{r.setRemoteAddr("203.0.113."+address.getAndIncrement());return r;})
    .param("username",u.getUsername()).param("password","incorrect"))
   .andExpect(redirectedUrl("/login?error"));
  var locked=users.findById(u.getId()).orElseThrow();
  assertThat(locked.getFailedLoginCount()).isEqualTo(5);assertThat(locked.getLoginLockedUntil()).isAfter(LocalDateTime.now().plusMinutes(14));
  var until=locked.getLoginLockedUntil();
  mvc.perform(post("/login").with(csrf()).param("username",u.getUsername()).param("password","SessionPass123!"))
   .andExpect(redirectedUrl("/login?error"));
  assertThat(users.findById(u.getId()).orElseThrow().getLoginLockedUntil()).isEqualTo(until);
  mvc.perform(get("/profile").session(active)).andExpect(status().isOk());
  locked.setLoginLockedUntil(LocalDateTime.now().minusSeconds(1));users.saveAndFlush(locked);
  login(u.getUsername(),"Unlocked browser");
  var fresh=users.findById(u.getId()).orElseThrow();assertThat(fresh.getFailedLoginCount()).isZero();assertThat(fresh.getLoginLockedUntil()).isNull();
  assertThat(sessions.history(u.getId(),0).getContent()).extracting(AccountSecurityEvent::getType)
   .contains(SecurityEventType.LOGIN_FAILURE,SecurityEventType.LOGIN_BLOCKED,SecurityEventType.LOGIN_SUCCESS);
 }
 @Test void newClientTriggersWebsiteWarningWithoutClaimingHardwareIdentity() throws Exception {
  var u=account("device_alert");
  login(u.getUsername(),"Browser one");assertThat(notices.countByOwnerUsernameAndReadAtIsNull(u.getUsername())).isZero();
  login(u.getUsername(),"Browser two");assertThat(notices.countByOwnerUsernameAndReadAtIsNull(u.getUsername())).isEqualTo(1);
 }
 @Test void eleventhLoginRevokesOldestAndAllSessionsRevocationIsAudited() throws Exception {
  var u=account("device_capacity");var first=login(u.getUsername(),"Browser");
  for(int i=0;i<10;i++)login(u.getUsername(),"Browser");
  assertThat(sessions.list(u,null)).hasSize(10);
  mvc.perform(get("/profile").session(first)).andExpect(status().isForbidden());
  settings.revokeSessions(u.getUsername(),"SessionPass123!");
  assertThat(sessions.list(users.findById(u.getId()).orElseThrow(),null)).isEmpty();
  assertThat(sessions.history(u.getId(),0).getContent()).extracting(AccountSecurityEvent::getType).contains(SecurityEventType.ALL_SESSIONS_REVOKED);
 }
 @Test void adminCanInspectButStaffAndOtherCustomersCannotAndHistoryIsPaged() throws Exception {
  var u=account("device_history");
  for(int i=0;i<21;i++)events.save(new AccountSecurityEvent(u.getId(),SecurityEventType.LOGIN_FAILURE,"192.0.2.20","History browser",LocalDateTime.now()));
  String path="/admin/users/"+u.getId()+"/security";
  mvc.perform(get(path).with(user("customer"))).andExpect(status().isForbidden());
  mvc.perform(get(path).with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
  mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
   .andExpect(model().attribute("hasNext",true)).andExpect(content().string(containsString(path+"?page=1")));
  mvc.perform(get(path).with(user("admin").roles("ADMIN")).param("page","1"))
   .andExpect(status().isOk()).andExpect(model().attribute("hasNext",false));
  mvc.perform(get(path).with(user("admin").roles("ADMIN")).param("page","-1")).andExpect(status().isBadRequest());
  mvc.perform(get("/profile/security")).andExpect(status().is3xxRedirection());
 }
 @Test void changingRoleBackNeverReactivatesTheOriginalSession() throws Exception {
  var actor=account("device_role_admin");actor.setRole("ADMIN");users.saveAndFlush(actor);
  var target=account("device_role_customer");var original=login(target.getUsername(),"Role browser");
  admin.setRole(actor.getUsername(),target.getId(),"STAFF");
  admin.setRole(actor.getUsername(),target.getId(),"USER");
  var fresh=users.findById(target.getId()).orElseThrow();
  assertThat(fresh.getAuthVersion()).isEqualTo(target.getAuthVersion()+2);
  assertThat(sessions.list(fresh,null)).isEmpty();
  mvc.perform(get("/profile").session(original)).andExpect(status().isForbidden());
  assertThat(sessions.history(target.getId(),0).getContent()).extracting(AccountSecurityEvent::getType)
   .contains(SecurityEventType.ACCOUNT_ROLE_CHANGED);
 }
 @Test void passwordChangeAuditsAndInvalidatesSessionRowsAndExistingBcryptRemainsUsable() throws Exception {
  var u=account("device_password");var session=login(u.getUsername(),"Browser");String hash=u.getPassword();
  assertThat(encoder.matches("SessionPass123!",hash)).isTrue();
  settings.changePassword(u.getUsername(),"SessionPass123!","ChangedPass123!","ChangedPass123!");
  var fresh=users.findById(u.getId()).orElseThrow();assertThat(sessions.list(fresh,null)).isEmpty();
  mvc.perform(get("/profile").session(session)).andExpect(status().isForbidden());
  assertThat(sessions.history(u.getId(),0).getContent()).extracting(AccountSecurityEvent::getType).contains(SecurityEventType.PASSWORD_CHANGED);
 }
}
