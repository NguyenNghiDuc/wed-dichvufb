package com.example.codeqlrepro;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain csrfEnabledFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .build();
    }
}
