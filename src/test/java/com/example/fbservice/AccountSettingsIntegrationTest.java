package com.example.fbservice;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.service.AccountSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Transactional
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:accountsettings;DB_CLOSE_DELAY=-1")
class AccountSettingsIntegrationTest {
 @Autowired UserRepository users; @Autowired AccountSettingsService settings;
 @Autowired PasswordEncoder encoder; @Autowired MockMvc mvc;
 User account(String name){User u=new User();u.setUsername(name);u.setPassword(encoder.encode("OldPass123!"));return users.saveAndFlush(u);}
 org.springframework.mock.web.MockHttpSession login(String username,String password) throws Exception {
  return (org.springframework.mock.web.MockHttpSession)mvc.perform(post("/login").with(csrf()).param("username",username).param("password",password))
   .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
 }
 @Test void updateNameIsOwnerScopedAndDoesNotChangeIdentity() throws Exception {
  var alice=account("settings_alice");var bob=account("settings_bob");
  mvc.perform(post("/profile/display-name").with(user(alice.getUsername())).with(csrf()).param("displayName","Tên mới"))
   .andExpect(redirectedUrl("/profile"));
  assertThat(alice.getDisplayName()).isEqualTo("Tên mới");assertThat(bob.getDisplayName()).isNull();
  assertThat(alice.getUsername()).isEqualTo("settings_alice");assertThat(alice.getRole()).isEqualTo("USER");
  mvc.perform(post("/profile/display-name").with(user(alice.getUsername())).param("displayName","No CSRF"))
   .andExpect(status().isForbidden());
 }
 @Test void passwordErrorsDoNotChangeHashOrExposePasswords() throws Exception {
  var a=account("settings_wrong");String hash=a.getPassword();
  mvc.perform(post("/profile/password").with(user(a.getUsername())).with(csrf()).param("currentPassword","wrong")
   .param("newPassword","NewPass123!").param("confirmation","NewPass123!"))
   .andExpect(status().isOk()).andExpect(model().attributeExists("error"));
  assertThat(a.getPassword()).isEqualTo(hash);assertThat(a.getAuthVersion()).isZero();
  assertThatThrownBy(()->settings.changePassword(a.getUsername(),"OldPass123!","NewPass123!","different"))
   .isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->settings.changePassword(a.getUsername(),"OldPass123!","ệ".repeat(30),"ệ".repeat(30)))
   .isInstanceOf(IllegalArgumentException.class);
 }
 @Test void passwordChangeHashesNewPasswordAndRejectsEveryOldPrincipal() throws Exception {
  var a=account("settings_change");var principal=new AccountPrincipal(a,"USER");
  var auth=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
  mvc.perform(post("/profile/password").session(login(a.getUsername(),"OldPass123!")).with(csrf())
   .param("currentPassword","OldPass123!").param("newPassword","NewPass123!").param("confirmation","NewPass123!"))
   .andExpect(redirectedUrl("/login?passwordChanged"));
  assertThat(encoder.matches("NewPass123!",a.getPassword())).isTrue();assertThat(a.getAuthVersion()).isEqualTo(1);
  mvc.perform(get("/profile").with(authentication(auth))).andExpect(status().isForbidden());
  mvc.perform(get("/profile").session(login(a.getUsername(),"NewPass123!")))
   .andExpect(status().isOk());
 }
 @Test void revokeRequiresCurrentPasswordAndInvalidatesPriorSessions() throws Exception {
  var a=account("settings_revoke");var principal=new AccountPrincipal(a,"USER");
  var auth=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
  assertThatThrownBy(()->settings.revokeSessions(a.getUsername(),"wrong")).isInstanceOf(IllegalArgumentException.class);
  mvc.perform(post("/profile/sessions/revoke").session(login(a.getUsername(),"OldPass123!")).with(csrf()).param("currentPassword","OldPass123!"))
   .andExpect(redirectedUrl("/login?sessionsClosed"));
  mvc.perform(get("/profile").with(authentication(auth))).andExpect(status().isForbidden());
  assertThat(encoder.matches("OldPass123!",a.getPassword())).isTrue();
 }
}
