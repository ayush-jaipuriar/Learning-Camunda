package com.example.disputeresolutionsystem.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationProvider;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CamundaAuthenticationProvider implements AuthenticationProvider {

    @Override
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            String name = authentication.getName();
            if (name != null && !name.isEmpty()) {
                return AuthenticationResult.successful(name);
            }
        }
        
        return AuthenticationResult.unsuccessful();
    }
    
    @Override
    public void augmentResponseByAuthenticationChallenge(HttpServletResponse response, ProcessEngine engine) {
        // No additional challenge needed as we're using Spring Security
    }
} 