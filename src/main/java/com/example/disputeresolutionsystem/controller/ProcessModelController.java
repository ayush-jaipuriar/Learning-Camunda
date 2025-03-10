package com.example.disputeresolutionsystem.controller;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing BPMN process models.
 */
@RestController
@RequestMapping("/api/process-models")
public class ProcessModelController {
    
    @Autowired
    private RepositoryService repositoryService;
    
    @Value("${camunda.bpm.deployment-resource-pattern:classpath*:**/*.bpmn}")
    private String deploymentResourcePattern;
    
    /**
     * Saves a BPMN model to the file system and deploys it to Camunda.
     * 
     * @param bpmnXml The BPMN XML content
     * @param processKey The process key (optional)
     * @return Response with status and deployment information
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveModel(
            @RequestBody String bpmnXml,
            @RequestParam(required = false) String processKey) {
        
        try {
            // If processKey is not provided, extract it from the XML
            if (processKey == null || processKey.isEmpty()) {
                // Simple extraction - in a real app, use proper XML parsing
                int idIndex = bpmnXml.indexOf("id=\"");
                if (idIndex > 0) {
                    int startIndex = idIndex + 4;
                    int endIndex = bpmnXml.indexOf("\"", startIndex);
                    if (endIndex > startIndex) {
                        processKey = bpmnXml.substring(startIndex, endIndex);
                    }
                }
                
                if (processKey == null || processKey.isEmpty()) {
                    processKey = "dispute_resolution_process";
                }
            }
            
            // Save to file system
            Path processesDir = Paths.get("src/main/resources/processes");
            if (!Files.exists(processesDir)) {
                Files.createDirectories(processesDir);
            }
            
            Path filePath = processesDir.resolve(processKey + ".bpmn");
            Files.write(filePath, bpmnXml.getBytes(StandardCharsets.UTF_8));
            
            // Deploy to Camunda
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
                    .name("Web Modeler Deployment - " + processKey)
                    .addInputStream(processKey + ".bpmn", 
                            new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));
            
            Deployment deployment = deploymentBuilder.deploy();
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("processKey", processKey);
            response.put("deploymentId", deployment.getId());
            response.put("deploymentName", deployment.getName());
            response.put("deploymentTime", deployment.getDeploymentTime());
            response.put("filePath", filePath.toString());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Loads a BPMN model from the file system.
     * 
     * @param processKey The process key
     * @return The BPMN XML content
     */
    @GetMapping("/load/{processKey}")
    public ResponseEntity<String> loadModel(@PathVariable String processKey) {
        try {
            Path filePath = Paths.get("src/main/resources/processes/" + processKey + ".bpmn");
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            String bpmnXml = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            return ResponseEntity.ok(bpmnXml);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    
    /**
     * Lists all available process models.
     * 
     * @return List of process models
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listModels() {
        try {
            Path processesDir = Paths.get("src/main/resources/processes");
            if (!Files.exists(processesDir)) {
                Files.createDirectories(processesDir);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("models", Files.list(processesDir)
                    .filter(path -> path.toString().endsWith(".bpmn"))
                    .map(path -> {
                        String filename = path.getFileName().toString();
                        return filename.substring(0, filename.lastIndexOf("."));
                    })
                    .toArray());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 