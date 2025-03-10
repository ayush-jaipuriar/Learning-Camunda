package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.CamundaUserService;
import com.example.disputeresolutionsystem.service.CaseAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
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

    @PostMapping("/reset-officers")
    @Transactional
    public ResponseEntity<Map<String, String>> resetOfficers() {
        // Get all officers before deleting
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        
        // Delete all existing officers
        caseOfficerRepository.deleteAll();
        
        // Delete corresponding Camunda users
        for (CaseOfficer officer : officers) {
            camundaUserService.deleteCamundaUser(officer.getUsername());
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All case officers have been deleted");
        
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
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All officer workloads have been reset to 0");
        
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
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All officers set to max capacity");
        
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