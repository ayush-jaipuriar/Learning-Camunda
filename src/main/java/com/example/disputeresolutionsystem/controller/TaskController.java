package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.CamundaUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final DisputeRepository disputeRepository;
    private final CaseOfficerRepository caseOfficerRepository;
    private final CamundaUserService camundaUserService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTasks(@RequestParam(required = false) String username) {
        List<Task> tasks;
        
        // Check if the user is an admin (supervisor)
        boolean isAdmin = false;
        if (username != null && !username.isEmpty()) {
            Optional<CaseOfficer> officerOpt = caseOfficerRepository.findByUsername(username);
            if (officerOpt.isPresent() && officerOpt.get().getLevel() == CaseOfficer.OfficerLevel.SUPERVISOR) {
                isAdmin = true;
            }
        }
        
        // If admin, show all tasks, otherwise filter by assignee
        if (isAdmin) {
            tasks = taskService.createTaskQuery().list();
        } else if (username != null && !username.isEmpty()) {
            // For regular users, only show tasks assigned to them
            tasks = taskService.createTaskQuery()
                    .taskAssignee(username)
                    .list();
        } else {
            // If no username provided, return empty list
            tasks = List.of();
        }
        
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
                    .filter(o -> o.getUsername().equals(assigneeUsername))
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
            
            // Ensure the officer is properly synced with Camunda
            camundaUserService.createCamundaUser(officer);
            
            // Set process variables - use sanitized username for Camunda
            taskService.setVariable(taskId, "assignedOfficerId", officer.getId());
            taskService.setVariable(taskId, "assignedOfficerUsername", camundaUserService.getSanitizedUsername(officer.getUsername()));
            taskService.setVariable(taskId, "assignedOfficerLevel", officer.getLevel().toString());
        }
        
        // Handle review completion
        if (task.getTaskDefinitionKey().equals("Activity_ReviewDispute")) {
            // Find the dispute
            Dispute dispute = disputeRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("Dispute not found: " + caseId));
            
            // Update PII validation status if provided
            if (variables.containsKey("piiValidationStatus")) {
                String status = variables.get("piiValidationStatus").toString();
                try {
                    Dispute.PIIValidationStatus piiStatus = Dispute.PIIValidationStatus.valueOf(status);
                    dispute.setPiiValidationStatus(piiStatus);
                    
                    // If there are PII notes, save them as well
                    if (variables.containsKey("piiNotes")) {
                        String notes = variables.get("piiNotes").toString();
                        // Save PII notes (could be added to a new field in Dispute or a separate table)
                        log.info("PII Validation Notes for {}: {}", caseId, notes);
                    }
                    
                    log.info("Updated PII validation status for dispute {} to {}", caseId, status);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid PII validation status: {}", status);
                }
            }
            
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
} 