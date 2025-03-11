package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.CamundaUserService;
import com.example.disputeresolutionsystem.service.CaseAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final CaseOfficerRepository caseOfficerRepository;
    private final DisputeRepository disputeRepository;
    private final CaseAssignmentService caseAssignmentService;
    private final TaskService taskService;
    private final CamundaUserService camundaUserService;
    private final IdentityService identityService;

    @PostMapping("/reset-officers")
    @Transactional
    public ResponseEntity<Map<String, String>> resetOfficers() {
        // Get all officers before deleting
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        
        // Delete all existing officers
        caseOfficerRepository.deleteAll();
        
        // Delete corresponding Camunda users and clean up identity resources
        for (CaseOfficer officer : officers) {
            camundaUserService.deleteCamundaUser(officer.getUsername());
        }
        
        // Also clean up any Camunda identity resources that might be lingering
        camundaUserService.cleanupAllIdentityResources();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All case officers have been deleted and Camunda identity resources cleaned up");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-officers")
    @Transactional
    public ResponseEntity<Map<String, Object>> createTestOfficers() {
        // Create test officers with valid Camunda usernames (only letters and numbers, starting with a letter)
        List<CaseOfficer> officers = Arrays.asList(
            createOfficer("johndoe", "John Doe", "john.doe@example.com", CaseOfficer.OfficerLevel.LEVEL_1, 0, 5),
            createOfficer("janesmith", "Jane Smith", "jane.smith@example.com", CaseOfficer.OfficerLevel.LEVEL_1, 0, 5),
            createOfficer("robertjohnson", "Robert Johnson", "robert.johnson@example.com", CaseOfficer.OfficerLevel.SENIOR, 0, 3),
            createOfficer("sarahwilliams", "Sarah Williams", "sarah.williams@example.com", CaseOfficer.OfficerLevel.SENIOR, 0, 3),
            createOfficer("michaelbrown", "Michael Brown", "michael.brown@example.com", CaseOfficer.OfficerLevel.SUPERVISOR, 0, 2)
        );
        
        // Save officers to database
        caseOfficerRepository.saveAll(officers);
        
        // Sync officers with Camunda users
        int syncCount = camundaUserService.syncOfficersWithCamundaUsers(officers);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test officers created successfully");
        response.put("officers", officers);
        response.put("syncedWithCamunda", syncCount);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-workloads")
    @Transactional
    public ResponseEntity<Map<String, String>> resetWorkloads() {
        // Reset all officer workloads to 0
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        for (CaseOfficer officer : officers) {
            officer.setCurrentWorkload(0);
        }
        caseOfficerRepository.saveAll(officers);
        
        // Also reset any task assignments in Camunda
        // This ensures UI is consistent with database state
        camundaUserService.syncOfficersWithCamundaUsers(officers);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All officer workloads have been reset to 0 and Camunda users synchronized");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/max-officer-capacity")
    @Transactional
    public ResponseEntity<Map<String, String>> setMaxOfficerCapacity() {
        // Set all officers to max capacity
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        for (CaseOfficer officer : officers) {
            officer.setCurrentWorkload(officer.getMaxWorkload());
        }
        caseOfficerRepository.saveAll(officers);
        
        // Also sync with Camunda to ensure UI is consistent
        camundaUserService.syncOfficersWithCamundaUsers(officers);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All officers set to max capacity and Camunda users synchronized");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-old-dispute")
    @Transactional
    public ResponseEntity<Map<String, Object>> createOldDispute() {
        // Create a dispute that's been unassigned for over 4 hours
        Dispute dispute = new Dispute();
        dispute.setCaseId("DRS-TEST-ESC");
        dispute.setUserId("user999");
        dispute.setDisputeType("test escalation");
        dispute.setCreditReportId("CR-99999");
        dispute.setStatus("Submitted");
        dispute.setPriorityLevel(Dispute.PriorityLevel.MEDIUM);
        dispute.setComplexityLevel(Dispute.ComplexityLevel.SIMPLE);
        dispute.setSubmissionTimestamp(LocalDateTime.now().minusHours(5));
        
        disputeRepository.save(dispute);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Old unassigned dispute created successfully");
        response.put("caseId", dispute.getCaseId());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trigger-escalation-check")
    public ResponseEntity<Map<String, String>> triggerEscalationCheck() {
        // Manually trigger the escalation check
        caseAssignmentService.checkForEscalations();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Escalation check triggered successfully");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulate-timer/{caseId}")
    @Transactional
    public ResponseEntity<Map<String, String>> simulateTimerExpiration(@PathVariable String caseId) {
        // Find the dispute
        Dispute dispute = disputeRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Dispute not found with ID: " + caseId));
        
        // Escalate the dispute (simulating timer expiration)
        boolean escalated = caseAssignmentService.escalateDispute(dispute);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", escalated ? "success" : "failed");
        response.put("message", escalated ? 
                "Timer expiration simulated, dispute escalated" : 
                "Failed to escalate dispute");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync-camunda-users")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncCamundaUsers() {
        // Get all officers
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        
        // Sync officers with Camunda
        int syncCount = camundaUserService.syncOfficersWithCamundaUsers(officers);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Camunda users synchronized successfully");
        response.put("syncedCount", syncCount);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-admin-user")
    @Transactional
    public ResponseEntity<Map<String, Object>> createAdminUser() {
        try {
            // Create admin user in Camunda
            User adminUser = identityService.createUserQuery()
                    .userId("admin")
                    .singleResult();
            
            if (adminUser == null) {
                // Create new admin user
                User newUser = identityService.newUser("admin");
                newUser.setFirstName("Admin");
                newUser.setLastName("User");
                newUser.setPassword("admin");
                newUser.setEmail("admin@example.com");
                identityService.saveUser(newUser);
                
                // Create camunda-admin group if it doesn't exist
                Group adminGroup = identityService.createGroupQuery()
                        .groupId("camunda-admin")
                        .singleResult();
                
                if (adminGroup == null) {
                    Group newGroup = identityService.newGroup("camunda-admin");
                    newGroup.setName("Camunda Admin");
                    newGroup.setType("SYSTEM");
                    identityService.saveGroup(newGroup);
                }
                
                // Add admin user to camunda-admin group
                try {
                    identityService.createMembership("admin", "camunda-admin");
                } catch (Exception e) {
                    log.warn("Error creating admin membership: {}", e.getMessage());
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Admin user created successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Admin user already exists");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error creating admin user", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error creating admin user: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private CaseOfficer createOfficer(String username, String fullName, String email, 
                                     CaseOfficer.OfficerLevel level, int currentWorkload, int maxWorkload) {
        CaseOfficer officer = new CaseOfficer();
        officer.setUsername(username);
        officer.setFullName(fullName);
        officer.setEmail(email);
        officer.setLevel(level);
        officer.setCurrentWorkload(currentWorkload);
        officer.setMaxWorkload(maxWorkload);
        return officer;
    }
} 