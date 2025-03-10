package com.example.disputeresolutionsystem.config;

import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 10)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // Disable CSRF protection for API endpoints
            .requestMatchers()
                .antMatchers("/camunda/**")
                .and()
            .authorizeRequests()
                .anyRequest().authenticated()
                .and()
            .httpBasic();
        
        return http.build();
    }
    
    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 9)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .requestMatchers()
                .antMatchers("/api/**", "/app/**", "/forms/**", "/static/**", "/**/*.js", "/**/*.css", "/**/*.html", "/**/*.png", "/**/*.jpg")
                .and()
            .authorizeRequests()
                .anyRequest().permitAll();
        
        return http.build();
    }
    
    @Bean
    public FilterRegistrationBean<ContainerBasedAuthenticationFilter> containerBasedAuthenticationFilter() {
        FilterRegistrationBean<ContainerBasedAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ContainerBasedAuthenticationFilter());
        registration.addInitParameter("authentication-provider", 
                "com.example.disputeresolutionsystem.config.CamundaAuthenticationProvider");
        registration.addUrlPatterns("/camunda/*");
        registration.setOrder(101);  // Make sure this filter is applied after the Spring Security filter
        return registration;
    }
    
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Collections.singletonList("*"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(0);  // Set a low order so this filter is applied first
        return bean;
    }
} 