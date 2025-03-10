package com.example.disputeresolutionsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving the BPMN modeler page.
 */
@Controller
public class ModelerController {
    
    /**
     * Returns the modeler page.
     * 
     * @return The name of the Thymeleaf template
     */
    @GetMapping("/modeler")
    public String getModelerPage() {
        return "modeler";
    }
} 