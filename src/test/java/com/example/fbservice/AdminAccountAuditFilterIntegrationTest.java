package com.example.fbservice;

import com.example.fbservice.admin.AdminAccountAudit;
import com.example.fbservice.admin.AdminAccountAuditRepository;
import com.example.fbservice.admin.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4auditscoped;DB_CLOSE_DELAY=-1")
class AdminAccountAuditFilterIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AdminUserService service;
    @Autowired AdminAccountAuditRepository repo;
    @BeforeEach void reset() { repo.deleteAll(); }

    @Test @WithMockUser(username="audit-admin",roles="ADMIN")
    void exactFiltersAndBoundedPaging() throws Exception {
        repo.save(new AdminAccountAudit("auditor-a",1001L,"ACCOUNT_ROLE","USER","STAFF"));
        repo.save(new AdminAccountAudit("auditor-b",1002L,"ACCOUNT_ENABLED","true","false"));
        repo.save(new AdminAccountAudit("auditor-a",1002L,"ACCOUNT_ENABLED","true","false"));
        var selected = service.pageAudit(0, "auditor-a", 1002L, "ACCOUNT_ENABLED");
        assertThat(selected.getTotalElements()).isEqualTo(1);
        assertThat(selected.getContent().get(0).getActor()).isEqualTo("auditor-a");
        mvc.perform(get("/admin/users/audit").param("actor","auditor-a")
            .param("targetId","1002").param("operation","ACCOUNT_ENABLED"))
            .andExpect(status().isOk()).andExpect(view().name("admin_user_audit"))
            .andExpect(model().attribute("events",hasSize(1)));
        mvc.perform(get("/admin/users/audit").param("actor","auditor"))
            .andExpect(status().isOk()).andExpect(model().attribute("events",empty()));
        for(int i=0;i<23;i++) repo.save(new AdminAccountAudit("auditor-bulk",
            9999L,"ACCOUNT_ROLE","USER","ADMIN"));
        assertThat(service.pageAudit(0,"auditor-bulk",9999L,"ACCOUNT_ROLE").getContent()).hasSize(20);
        assertThat(service.pageAudit(1,"auditor-bulk",9999L,"ACCOUNT_ROLE").getContent()).hasSize(3);
    }
    @Test @WithMockUser(username="audit-admin",roles="ADMIN")
    void rejectsMalformedFilters() throws Exception {
        mvc.perform(get("/admin/users/audit").param("actor","%")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/users/audit").param("targetId","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/users/audit").param("operation","DELETE_ALL")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/users/audit").param("page","10001")).andExpect(status().isBadRequest());
    }
    @Test void roleIsolation() throws Exception {
        mvc.perform(get("/admin/users/audit")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/users/audit").with(user("some-staff").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/users/audit").with(user("some-user").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
