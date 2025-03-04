package com.example.disputeresolutionsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;

@SpringBootApplication
@EntityScan("com.example.disputeresolutionsystem.model")
@EnableJpaRepositories("com.example.disputeresolutionsystem.repository")
@EnableProcessApplication
public class DisputeResolutionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisputeResolutionApplication.class, args);
    }
} 