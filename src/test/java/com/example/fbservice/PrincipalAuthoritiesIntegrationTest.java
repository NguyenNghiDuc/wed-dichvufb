package com.example.fbservice;

import com.example.fbservice.config.AccountPrincipal;
import com.example.fbservice.model.User;
import com.example.fbservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:extraauthorities;DB_CLOSE_DELAY=-1")
class PrincipalAuthoritiesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test
    void sessionWithExtraRoleIsNotAllowedToUseAdministrativePrivileges() throws Exception {
        User u = new User();
        u.setUsername("extra-authority-user");
        u.setRole("USER");
        u.setPassword(passwords.encode("a-valid-password"));
        u=users.saveAndFlush(u);

        var user=new AccountPrincipal(u, "USER");
        var extra = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

        mvc.perform(get("/admin/dashboard").with(authentication(extra)))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/login?sessionExpired"));
    }
}
