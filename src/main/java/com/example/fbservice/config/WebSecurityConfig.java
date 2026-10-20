package com.example.fbservice.config;

import com.example.fbservice.repository.UserRepository;
import com.example.fbservice.security.*;
import java.time.Clock;
import jakarta.servlet.DispatcherType;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users, AccountSessionService sessions,
            GuardedAuthenticationProvider provider, TrackedLoginSuccessHandler loginSuccess) throws Exception {
        return http
            .authenticationProvider(provider)
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                // The signed machine-to-machine callback is the ONLY CSRF exception.
                // Restrict it to POST; all other state-changing methods remain protected.
                new AntPathRequestMatcher("/api/v1/payments/webhooks/internal", "POST"),
                new AntPathRequestMatcher("/api/v1/payments/webhooks/bank-settled", "POST")))
            .exceptionHandling(errors -> errors.accessDeniedHandler(new AuthenticationAccessDeniedHandler()))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                    "/images/growlab-workflow.svg", "/images/growlab-compare.svg").permitAll()
                .requestMatchers("/dich-vu/**").permitAll()
                .requestMatchers("/", "/login", "/register", "/help", "/style.css", "/landing.js", "/site.js", "/favicon.ico",
                    "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/payments/webhooks/internal",
                    "/api/v1/payments/webhooks/bank-settled").permitAll()
                .requestMatchers("/admin/support/**", "/staff/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; " +
                "form-action 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:")))
            .addFilterBefore(new AccountStatusFilter(users,sessions), AuthorizationFilter.class)
            .formLogin(form -> form.loginPage("/login").authenticationDetailsSource(LoginRequestDetails::new)
                .successHandler(loginSuccess).permitAll())
            .logout(logout -> logout.addLogoutHandler((request,response,authentication)->{
                if(authentication!=null&&authentication.getPrincipal() instanceof AccountPrincipal principal)
                    sessions.logout(principal,request);
            }).logoutSuccessUrl("/").permitAll())
            .build();
    }

    @Bean
    public Clock securityClock(){return Clock.systemUTC();}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
