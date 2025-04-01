package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.dto.DisputeSubmissionDTO;
import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.UserPII;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.CamundaUserService;
import com.example.disputeresolutionsystem.service.CaseAssignmentService;
import com.example.disputeresolutionsystem.service.DisputeService;
import com.example.disputeresolutionsystem.service.UserPIIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final UserPIIService userPIIService;
    private final DisputeService disputeService;
    private final RuntimeService runtimeService;

    @PostMapping("/reset-officers")
    @Transactional
    public ResponseEntity<Map<String, String>> resetOfficers() {
        // Get all officers before deleting
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        
        // Delete PII data for all officers first
        for (CaseOfficer officer : officers) {
            userPIIService.deletePIIForOfficer(officer.getId());
        }
        
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
        response.put("message", "All case officers and associated PII data have been deleted and Camunda identity resources cleaned up");
        
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
        
        // Create dummy PII data for each officer
        for (CaseOfficer officer : officers) {
            userPIIService.createDummyPIIForOfficer(officer);
        }
        
        // Sync officers with Camunda users
        int syncCount = camundaUserService.syncOfficersWithCamundaUsers(officers);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test officers created successfully with dummy PII data");
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

    @PostMapping("/monitor-sla")
    @jakarta.transaction.Transactional
    public ResponseEntity<Map<String, String>> monitorSLA() {
        // Trigger SLA monitoring manually
        caseAssignmentService.monitorSLAViolations();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "SLA monitoring triggered successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/set-sla-deadline/{caseId}/{minutesFromNow}")
    @jakarta.transaction.Transactional
    public ResponseEntity<Map<String, Object>> setSLADeadline(
            @PathVariable String caseId,
            @PathVariable int minutesFromNow) {
        
        // Find the dispute
        Dispute dispute = disputeRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Dispute not found with ID: " + caseId));
        
        // Set SLA deadline
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(minutesFromNow);
        dispute.setSlaDeadline(deadline);
        disputeRepository.save(dispute);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "SLA deadline set successfully");
        response.put("caseId", caseId);
        response.put("slaDeadline", deadline.toString());
        response.put("minutesFromNow", minutesFromNow);
        
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

    @PostMapping("/create-test-users")
    @Transactional
    public ResponseEntity<Map<String, Object>> createTestUsers() {
        log.info("Creating test users with PII data");
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> createdUsers = new ArrayList<>();
        
        // Get all officers
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        if (officers.isEmpty()) {
            response.put("status", "error");
            response.put("message", "No officers found. Please create officers first.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Get the first officer to associate with the test users
        CaseOfficer officer = officers.get(0);
        
        // Create john_user1 with exact matching data from the sample
        UserPII johnUser = new UserPII();
        johnUser.setUsername("john_user1");
        johnUser.setFullName("John Doe");
        johnUser.setAddress("123 Main St, Anytown, CA 92345");
        johnUser.setPhoneNumber("555-123-4567");
        johnUser.setEmailAddress("john_user1@example.com");
        johnUser.setSsn("123-45-6789");
        johnUser.setDateOfBirth("01/15/1980");
        johnUser.setCaseOfficer(officer);
        userPIIService.saveUserPII(johnUser);
        
        Map<String, String> johnUserMap = new HashMap<>();
        johnUserMap.put("username", johnUser.getUsername());
        johnUserMap.put("fullName", johnUser.getFullName());
        createdUsers.add(johnUserMap);
        
        // Create jane_user2 with exact matching data from the sample
        UserPII janeUser = new UserPII();
        janeUser.setUsername("jane_user2");
        janeUser.setFullName("Jane Smith");
        janeUser.setAddress("456 Oak Ave, Springfield, IL 62701");
        janeUser.setPhoneNumber("555-234-5678");
        janeUser.setEmailAddress("jane_user2@example.com");
        janeUser.setSsn("234-56-7890");
        janeUser.setDateOfBirth("02/20/1985");
        janeUser.setCaseOfficer(officer);
        userPIIService.saveUserPII(janeUser);
        
        Map<String, String> janeUserMap = new HashMap<>();
        janeUserMap.put("username", janeUser.getUsername());
        janeUserMap.put("fullName", janeUser.getFullName());
        createdUsers.add(janeUserMap);
        
        // Create mike_user3 with exact matching data from the sample
        UserPII mikeUser = new UserPII();
        mikeUser.setUsername("mike_user3");
        mikeUser.setFullName("Michael Johnson");
        mikeUser.setAddress("789 Pine Rd, Liberty, NY 10001");
        mikeUser.setPhoneNumber("555-345-6789");
        mikeUser.setEmailAddress("mike_user3@example.com");
        mikeUser.setSsn("345-67-8901");
        mikeUser.setDateOfBirth("03/25/1990");
        mikeUser.setCaseOfficer(officer);
        userPIIService.saveUserPII(mikeUser);
        
        Map<String, String> mikeUserMap = new HashMap<>();
        mikeUserMap.put("username", mikeUser.getUsername());
        mikeUserMap.put("fullName", mikeUser.getFullName());
        createdUsers.add(mikeUserMap);
        
        response.put("status", "success");
        response.put("message", "Created test users with PII data");
        response.put("users", createdUsers);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create a test high-complexity dispute for multi-level approval testing
     */
    @PostMapping("/high-complexity-dispute")
    public ResponseEntity<Map<String, Object>> createHighComplexityDispute(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        log.info("Creating test high-complexity dispute for multi-level approval");
        
        // Create a dispute DTO
        DisputeSubmissionDTO disputeDTO = new DisputeSubmissionDTO();
        disputeDTO.setUserId("test_user");
        disputeDTO.setDisputeType("Fraud Alert");
        disputeDTO.setCreditReportId("CR-" + System.currentTimeMillis());
        disputeDTO.setUserFullName("John Test Doe");
        disputeDTO.setUserAddress("456 High Risk Ave, Complexville, CA 98765");
        disputeDTO.setUserPhoneNumber("555-987-6543");
        disputeDTO.setUserEmailAddress("high_risk@example.com");
        disputeDTO.setDescription("This is a high-complexity dispute for testing the multi-level approval process");
        
        List<MultipartFile> files = new ArrayList<>();
        if (file != null && !file.isEmpty()) {
            files.add(file);
            log.info("Received document with size: {} bytes, name: {}", file.getSize(), file.getOriginalFilename());
        }
        
        // Create the dispute
        Dispute dispute = disputeService.createDispute(disputeDTO, files);
        
        // Force high complexity if not already set
        if (dispute.getComplexityLevel() != Dispute.ComplexityLevel.HIGH_RISK) {
            dispute.setComplexityLevel(Dispute.ComplexityLevel.HIGH_RISK);
            dispute.setPriorityLevel(Dispute.PriorityLevel.HIGH);
            disputeRepository.save(dispute);
            log.info("Forced complexity level to HIGH_RISK for testing");
        }
        
        // Get the process instance ID
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(dispute.getCaseId())
                .singleResult();
        
        String processInstanceId = processInstance != null ? processInstance.getId() : null;
        
        Map<String, Object> response = new HashMap<>();
        response.put("caseId", dispute.getCaseId());
        response.put("processId", processInstanceId);
        response.put("status", dispute.getStatus());
        response.put("complexityLevel", dispute.getComplexityLevel().toString());
        
        return ResponseEntity.ok(response);
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