package com.example.fbservice;

import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w4staffcsv;DB_CLOSE_DELAY=-1")
class AdminStaffDirectoryCsvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @BeforeEach void clean() { users.deleteAll(); }

    private void account(String name, String role, boolean enabled) {
        User u = new User();
        u.setUsername(name);
        u.setPassword("fake-hash-not-used-for-auth-in-this-test");
        u.setRole(role);
        u.setEnabled(enabled);
        users.save(u);
    }

    @Test @WithMockUser(username="w4-csv-admin",roles="ADMIN")
    void exportIsBoundedFilteredAndContainsNoPrivateAccountFields() throws Exception {
        for (int i=0;i<23;i++) account("w4-csv-staff-"+i,"STAFF",true);
        account("w4-csv-locked","STAFF",false);
        account("w4-csv-customer","USER",true);
        account("w4-csv-otheradmin","ADMIN",true);

        var first=mvc.perform(get("/admin/staff/export.csv")
                .param("role","STAFF").param("enabled","true").param("page","0"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(header().string("X-Content-Type-Options","nosniff"))
            .andExpect(header().string("Content-Disposition",containsString("attachment")))
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(first).contains("username,role,enabled,active_orders")
            .doesNotContain("w4-csv-locked","w4-csv-customer","w4-csv-otheradmin",
                "password","email","fake-hash-not-used-for-auth");
        assertThat(first.split("\\r\\n").length).isEqualTo(21);

        var second=mvc.perform(get("/admin/staff/export.csv")
                .param("role","STAFF").param("enabled","true").param("page","1"))
            .andExpect(status().isOk()).andReturn().getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        assertThat(second.split("\\r\\n").length).isEqualTo(4);
        mvc.perform(get("/admin/staff/export.csv")
                .param("username","w4-csv-locked").param("enabled","false"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("w4-csv-locked")));
    }

    @Test @WithMockUser(username="w4-csv-admin",roles="ADMIN")
    void malformedFilterAndPageFailClosed() throws Exception {
        mvc.perform(get("/admin/staff/export.csv").param("role","OWNER"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff/export.csv").param("username","%"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff/export.csv").param("enabled","sometimes"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff/export.csv").param("page","-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/staff/export.csv").param("page","10001"))
            .andExpect(status().isBadRequest());
    }

    @Test void staffAndCustomersCannotExportDirectory() throws Exception {
        mvc.perform(get("/admin/staff/export.csv"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/staff/export.csv").with(user("w4-worker").roles("STAFF")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/staff/export.csv").with(user("w4-customer").roles("USER")))
            .andExpect(status().isForbidden());
    }
}
