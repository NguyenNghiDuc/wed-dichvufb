package com.example.fbservice.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Message bodies, ticket contents and account-specific thread indexes must not
 * persist in shared or browser HTTP caches. Covers both HTML pages and REST
 * endpoints without changing authentication or payment controllers.
 */
@Configuration
public class SupportPrivateCacheConfig implements WebMvcConfigurer {
    private static final HandlerInterceptor PRIVATE_CACHE = new HandlerInterceptor() {
        @Override
        public boolean preHandle(HttpServletRequest request,
                                 HttpServletResponse response, Object handler) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            return true;
        }
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(PRIVATE_CACHE)
                .addPathPatterns("/chat", "/chat/**", "/support", "/support/**",
                        "/admin/support", "/admin/support/**");
    }
}
