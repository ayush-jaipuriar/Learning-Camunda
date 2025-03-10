package com.example.disputeresolutionsystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        if (principal != null) {
            response.put("authenticated", true);
            response.put("username", principal.getName());
        } else {
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/login-success")
    public ResponseEntity<Map<String, Object>> loginSuccess(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Login successful");
        response.put("username", principal != null ? principal.getName() : "anonymous");
        return ResponseEntity.ok(response);
    }
} 