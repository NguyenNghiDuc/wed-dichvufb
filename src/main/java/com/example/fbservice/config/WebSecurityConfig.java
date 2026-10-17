package com.example.fbservice.config;

import com.example.fbservice.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class WebSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users) throws Exception {
        return http
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                // The signed machine-to-machine callback is the ONLY CSRF exception.
                // Restrict it to POST; all other state-changing methods remain protected.
                new AntPathRequestMatcher("/api/v1/payments/webhooks/internal", "POST")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/help", "/style.css", "/favicon.ico",
                    "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/payments/webhooks/internal").permitAll()
                .requestMatchers("/admin/support/**", "/staff/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; " +
                "form-action 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:")))
            .addFilterBefore(new AccountStatusFilter(users), AuthorizationFilter.class)
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/campaigns", false).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
