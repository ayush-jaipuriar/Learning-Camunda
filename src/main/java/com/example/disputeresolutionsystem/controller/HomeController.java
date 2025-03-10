package com.example.disputeresolutionsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the home/welcome page.
 */
@Controller
public class HomeController {
    
    /**
     * Returns the welcome page.
     * 
     * @return The name of the Thymeleaf template
     */
    @GetMapping("/")
    public String getHomePage() {
        return "index";
    }
} 