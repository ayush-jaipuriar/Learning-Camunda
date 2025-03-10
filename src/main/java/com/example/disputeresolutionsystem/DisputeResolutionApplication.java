package com.example.disputeresolutionsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EntityScan("com.example.disputeresolutionsystem.model")
@EnableJpaRepositories("com.example.disputeresolutionsystem.repository")
@EnableProcessApplication
@EnableScheduling
public class DisputeResolutionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisputeResolutionApplication.class, args);
    }
    
    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/forms/**")
                        .addResourceLocations("classpath:/static/forms/");
                registry.addResourceHandler("/app/forms/**")
                        .addResourceLocations("classpath:/static/forms/");
            }
        };
    }
} 