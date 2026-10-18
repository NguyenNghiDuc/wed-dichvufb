package com.example.fbservice.config;

import com.example.fbservice.repository.UserRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class WebSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users, AuthenticationRateLimitFilter loginLimiter,
                                                   LoginAuditSuccessHandler loginAuditSuccessHandler) throws Exception {
        return http
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                // The signed machine-to-machine callback is the ONLY CSRF exception.
                // Restrict it to POST; all other state-changing methods remain protected.
                new AntPathRequestMatcher("/api/v1/payments/webhooks/internal", "POST")))
            .authorizeHttpRequests(auth -> auth
                // Spring Boot forwards 404/500 requests to /error; never save them as login targets.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/", "/login", "/register", "/help", "/error", "/style.css",
                    "/favicon.ico", "/favicon.svg", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/payments/webhooks/internal").permitAll()
                .requestMatchers("/admin/support/**", "/staff/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; " +
                "form-action 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:")))
            .addFilterBefore(loginLimiter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new AccountStatusFilter(users), AuthorizationFilter.class)
            .formLogin(form -> form.loginPage("/login").successHandler(loginAuditSuccessHandler).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            .build();
    }

    /** Register in the Spring Security chain only, after CsrfFilter. */
    @Bean
    public FilterRegistrationBean<AuthenticationRateLimitFilter> authenticationLimiterServletRegistration(
            AuthenticationRateLimitFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
