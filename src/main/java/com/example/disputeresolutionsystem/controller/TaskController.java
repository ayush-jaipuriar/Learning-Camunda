package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final DisputeRepository disputeRepository;
    private final CaseOfficerRepository caseOfficerRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTasks() {
        List<Task> tasks = taskService.createTaskQuery().list();
        
        List<Map<String, Object>> taskDetails = tasks.stream()
                .map(task -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("id", task.getId());
                    details.put("name", task.getName());
                    details.put("assignee", task.getAssignee());
                    details.put("created", task.getCreateTime());
                    details.put("processInstanceId", task.getProcessInstanceId());
                    
                    // Get business key (case ID)
                    String caseId = taskService.getVariable(task.getId(), "caseId") != null ?
                            taskService.getVariable(task.getId(), "caseId").toString() : null;
                    details.put("caseId", caseId);
                    
                    return details;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(taskDetails);
    }

    @GetMapping("/by-case/{caseId}")
    public ResponseEntity<Map<String, Object>> getTaskByCaseId(@PathVariable String caseId) {
        // Find task by business key (case ID)
        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("caseId", caseId)
                .singleResult();
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> taskDetails = new HashMap<>();
        taskDetails.put("taskId", task.getId());
        taskDetails.put("name", task.getName());
        taskDetails.put("assignee", task.getAssignee());
        taskDetails.put("created", task.getCreateTime());
        
        return ResponseEntity.ok(taskDetails);
    }

    @GetMapping("/manual-assignments")
    public ResponseEntity<Map<String, Object>> getManualAssignmentTasks() {
        // Find manual assignment tasks
        Task task = taskService.createTaskQuery()
                .taskDefinitionKey("Activity_ManualIntervention")
                .singleResult();
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> taskDetails = new HashMap<>();
        taskDetails.put("taskId", task.getId());
        taskDetails.put("name", task.getName());
        taskDetails.put("caseId", taskService.getVariable(task.getId(), "caseId"));
        
        return ResponseEntity.ok(taskDetails);
    }

    @PostMapping("/{taskId}/complete")
    @Transactional
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> variables) {
        
        // Get the task
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Get case ID
        String caseId = taskService.getVariable(taskId, "caseId").toString();
        
        // Handle manual assignment
        if (task.getTaskDefinitionKey().equals("Activity_ManualIntervention") && variables.containsKey("manualAssignee")) {
            String assigneeUsername = variables.get("manualAssignee").toString();
            
            // Find the officer by username
            CaseOfficer officer = caseOfficerRepository.findAll().stream()
                    .filter(o -> o.getUsername().equals(assigneeUsername) || 
                                 sanitizeUsername(o.getUsername()).equals(assigneeUsername))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Officer not found: " + assigneeUsername));
            
            // Find the dispute
            Dispute dispute = disputeRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("Dispute not found: " + caseId));
            
            // Assign the dispute
            dispute.setAssignedOfficer(officer);
            dispute.setAssignmentTimestamp(java.time.LocalDateTime.now());
            dispute.setStatus("Manually Assigned");
            
            // Update officer workload
            officer.setCurrentWorkload(officer.getCurrentWorkload() + 1);
            
            // Save changes
            caseOfficerRepository.save(officer);
            disputeRepository.save(dispute);
            
            // Set process variables - use sanitized username for Camunda
            String sanitizedUsername = sanitizeUsername(officer.getUsername());
            taskService.setVariable(taskId, "assignedOfficerId", officer.getId());
            taskService.setVariable(taskId, "assignedOfficerUsername", sanitizedUsername);
            taskService.setVariable(taskId, "assignedOfficerLevel", officer.getLevel().toString());
        }
        
        // Handle review completion
        if (task.getTaskDefinitionKey().equals("Activity_ReviewDispute")) {
            // Find the dispute
            Dispute dispute = disputeRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("Dispute not found: " + caseId));
            
            // Update dispute status based on decision
            if (variables.containsKey("reviewDecision")) {
                String decision = variables.get("reviewDecision").toString();
                dispute.setStatus("Reviewed - " + decision);
                dispute.setResolutionTimestamp(java.time.LocalDateTime.now());
                
                // Save changes
                disputeRepository.save(dispute);
            }
        }
        
        // Complete the task with variables
        taskService.complete(taskId, variables);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Task completed successfully");
        response.put("taskId", taskId);
        response.put("caseId", caseId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Sanitize username to ensure it's valid for Camunda
     */
    private String sanitizeUsername(String username) {
        // First, replace dots and other special characters with underscores
        String sanitized = username.replaceAll("[^a-zA-Z0-9]", "_");
        
        // Ensure the ID starts with a letter (if it starts with a number, prepend 'user_')
        if (sanitized.length() > 0 && !Character.isLetter(sanitized.charAt(0)) && sanitized.charAt(0) != '_') {
            sanitized = "user_" + sanitized;
        }
        
        // Ensure the ID is not empty
        if (sanitized.isEmpty()) {
            sanitized = "user_" + System.currentTimeMillis();
        }
        
        return sanitized;
    }
} 