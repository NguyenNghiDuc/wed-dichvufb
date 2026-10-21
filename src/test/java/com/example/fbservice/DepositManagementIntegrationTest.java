package com.example.fbservice;

import com.example.fbservice.model.DepositRequest;
import com.example.fbservice.repository.DepositRequestRepository;
import com.example.fbservice.service.DepositService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:depositmanagement;DB_CLOSE_DELAY=-1")
class DepositManagementIntegrationTest {
 @Autowired com.example.fbservice.repository.UserRepository users;
 @Autowired MockMvc mvc; @Autowired DepositService deposits; @Autowired DepositRequestRepository repo;
 void owner(String name){if(!users.existsByUsername(name)){var u=new com.example.fbservice.model.User();u.setUsername(name);u.setPassword("unused-test-fixture");users.saveAndFlush(u);}}
 DepositRequest request(String owner){owner(owner);var d=new DepositRequest();d.setAmount(200000);d.setNote("Nạp tiền cho chiến dịch");return deposits.save(d,owner);}
 @Test void ownersSeeDetailsButCannotReadOrCancelAnotherAccountsRequest() throws Exception {
  var d=request("deposit_owner");
  mvc.perform(get("/deposit/"+d.getId()).with(user("deposit_owner"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("200.000 đ")));
  mvc.perform(get("/deposit/"+d.getId()).with(user("other"))).andExpect(status().isNotFound());
  mvc.perform(post("/deposit/"+d.getId()+"/cancel").with(user("other")).with(csrf())).andExpect(status().isNotFound());
  assertThat(repo.findById(d.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
 }
 @Test void cancellationRequiresCsrfAndOnlyPendingRequestsCanBeCancelled() throws Exception {
  var d=request("cancel_owner");String path="/deposit/"+d.getId()+"/cancel";
  mvc.perform(post(path).with(user("cancel_owner"))).andExpect(status().isForbidden());
  mvc.perform(post(path).with(user("cancel_owner")).with(csrf())).andExpect(redirectedUrl("/deposit/"+d.getId()));
  assertThat(repo.findById(d.getId()).orElseThrow().getStatus()).isEqualTo("CANCELLED");
  mvc.perform(post(path).with(user("cancel_owner")).with(csrf())).andExpect(status().isConflict());
  var approved=request("cancel_owner");approved.setStatus("APPROVED");repo.saveAndFlush(approved);
  mvc.perform(post("/deposit/"+approved.getId()+"/cancel").with(user("cancel_owner")).with(csrf())).andExpect(status().isConflict());
 }
 @Test void pagedListsRemainOwnerScopedAndValidateFilters() throws Exception {
  for(int i=0;i<22;i++)request("page_owner");request("private_other");
  var result=mvc.perform(get("/deposit/list").with(user("page_owner"))).andExpect(status().isOk()).andReturn();
  var records=(org.springframework.data.domain.Page<?>)result.getModelAndView().getModel().get("records");
  assertThat(records.getTotalElements()).isEqualTo(22);assertThat(records.getContent()).hasSize(20);
  mvc.perform(get("/deposit/list").param("page","1").param("status","PENDING").with(user("page_owner"))).andExpect(status().isOk());
  mvc.perform(get("/deposit/list").param("status","CANCELLED").with(user("page_owner"))).andExpect(status().isOk());
  mvc.perform(get("/deposit/list").param("status","fake").with(user("page_owner"))).andExpect(status().isBadRequest());
  mvc.perform(get("/deposit/list").param("page","-1").with(user("page_owner"))).andExpect(status().isBadRequest());
 }
 @Test void administrationIsRestrictedAndCanFilterAnExactOwner() throws Exception {
  request("admin_lookup");request("not_lookup");
  mvc.perform(get("/admin/deposits").with(user("customer"))).andExpect(status().isForbidden());
  mvc.perform(get("/admin/deposits").with(user("staff").roles("STAFF"))).andExpect(status().isForbidden());
  var result=mvc.perform(get("/admin/deposits").param("owner","admin_lookup").param("status","PENDING").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn();
  var records=(org.springframework.data.domain.Page<?>)result.getModelAndView().getModel().get("records");
  assertThat(records.getTotalElements()).isEqualTo(1);
 }
 @Test void createRedirectsToHistoryWithAnExplicitPendingConfirmation() throws Exception {
  owner("new_owner");
  mvc.perform(post("/deposit/create").with(user("new_owner")).with(csrf()).param("amount","50000").param("status","APPROVED").param("ownerUsername","victim"))
   .andExpect(redirectedUrl("/deposit/list")).andExpect(flash().attributeExists("success"));
  var saved=repo.findByOwnerUsernameOrderByCreatedAtDesc("new_owner");assertThat(saved).hasSize(1);assertThat(saved.get(0).getStatus()).isEqualTo("PENDING");
 }
}
