package com.example.fbservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** All business endpoints require authentication; CSRF is disabled only for verified HMAC webhooks. */
@Configuration
public class WebSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/payments/webhooks/internal"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/style.css", "/favicon.ico",
                    "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/payments/webhooks/internal").permitAll()
                .requestMatchers("/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; " +
                "form-action 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:")))
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/campaigns", false).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
