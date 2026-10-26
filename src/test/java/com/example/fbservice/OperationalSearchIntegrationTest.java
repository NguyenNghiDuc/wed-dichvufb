package com.example.fbservice;
import com.example.fbservice.admin.AdminUserService;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.support.*;
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
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:operationalsearch;DB_CLOSE_DELAY=-1")
class OperationalSearchIntegrationTest {
 @Autowired MockMvc mvc; @Autowired UserRepository users;
 @Autowired AdminUserService accounts; @Autowired SupportTicketService support;
 User account(String name,String role){User u=new User();u.setUsername(name);u.setPassword("test-unused");u.setRole(role);return users.save(u);}
 @Test void accountSearchCombinesRoleStateAndLiteralCaseInsensitiveFields(){
  var target=account("search_target","STAFF");target.setDisplayName("Sale 50%_off");target.setEmail("Target@Example.com");
  var decoy=account("search_other","STAFF");decoy.setDisplayName("Sale 50AAoff");
  assertThat(accounts.page(0,"50%_OFF","STAFF",true).getContent()).extracting(User::getId).containsExactly(target.getId());
  assertThat(accounts.page(0,"target@example.com",null,null).getTotalElements()).isEqualTo(1);
  assertThat(accounts.page(0,"50%_off","USER",true).isEmpty()).isTrue();
  target.setEnabled(false);
  assertThat(accounts.page(0,"search_target",null,false).getTotalElements()).isEqualTo(1);
 }
 @Test void supportSearchCannotOverrideOwnerAndSupportsLiteralIdAndStatus() throws Exception {
  var mine=support.create("search_alice","Giảm 50%_off","Mô tả yêu cầu của khách hàng.");
  support.create("search_bob","Giảm 50%_off","Mô tả yêu cầu của khách hàng.");
  support.create("search_alice","Giảm 50XXoff","Mô tả yêu cầu của khách hàng.");
  support.closeAsOwner("search_alice",mine.getId());
  assertThat(support.searchMine("search_alice",0,"50%_OFF",mine.getId(),TicketStatus.CLOSED).getContent()).extracting(SupportTicket::getId).containsExactly(mine.getId());
  assertThat(support.searchForAdmin(0,"50%_off",null,TicketStatus.OPEN,"search_alice").isEmpty()).isTrue();
  mvc.perform(get("/support").with(user("search_alice")).param("owner","search_bob").param("q","50%_off"))
   .andExpect(status().isOk()).andExpect(model().attribute("totalElements",1L));
 }
 @Test void filteredViewsRenderAndPaginationKeepsFilters() throws Exception {
  for(int i=0;i<21;i++)account("paging_search_"+i,"USER");
  mvc.perform(get("/admin/users").with(user("operator").roles("ADMIN")).param("q","paging_search_").param("role","USER").param("enabled","true"))
   .andExpect(status().isOk()).andExpect(model().attribute("totalElements",21L))
   .andExpect(content().string(containsString("page=1&amp;q=paging_search_&amp;role=USER&amp;enabled=true")));
  mvc.perform(get("/admin/support").with(user("staff").roles("STAFF")).param("status","OPEN").param("owner","search_alice"))
   .andExpect(status().isOk()).andExpect(model().attribute("ownerFilter","search_alice"));
 }
 @Test void mutationsRetainFiltersAndRejectBadNavigationBeforeWriting() throws Exception {
  account("search_admin","ADMIN");var target=account("search_change","USER");
  mvc.perform(post("/admin/users/"+target.getId()+"/enabled").with(user("search_admin").roles("ADMIN")).with(csrf())
    .param("enabled","false").param("page","1").param("q","search").param("filterRole","USER").param("filterEnabled","true"))
   .andExpect(redirectedUrl("/admin/users?page=1&q=search&role=USER&enabled=true")).andExpect(flash().attributeExists("success"));
  mvc.perform(post("/admin/users/"+target.getId()+"/role").with(user("search_admin").roles("ADMIN")).with(csrf())
    .param("role","STAFF").param("page","-1")).andExpect(status().isBadRequest());
  assertThat(target.getRole()).isEqualTo("USER");
 }
 @Test void invalidFiltersAndUnauthorizedAccessAreRejected() throws Exception {
  mvc.perform(get("/admin/users").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/users").with(user("admin").roles("ADMIN")).param("role","ROOT")).andExpect(status().isBadRequest());
  mvc.perform(get("/support").with(user("customer")).param("ticketId","-1")).andExpect(status().isBadRequest());
  mvc.perform(get("/support").with(user("customer")).param("q","x".repeat(121))).andExpect(status().isBadRequest());
  mvc.perform(get("/support").with(user("customer")).param("status","INVALID")).andExpect(status().isBadRequest());
 }
}
