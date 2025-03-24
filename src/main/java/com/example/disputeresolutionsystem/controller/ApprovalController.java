package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.Dispute.ApprovalStatus;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.MultiLevelApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final MultiLevelApprovalService approvalService;
    private final DisputeRepository disputeRepository;

    /**
     * Get the approval status for a dispute
     */
    @GetMapping("/{caseId}/status")
    public ResponseEntity<?> getApprovalStatus(@PathVariable String caseId) {
        log.info("Getting approval status for dispute: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Dispute dispute = disputeOpt.get();
        Map<String, Object> response = new HashMap<>();
        
        // Level 1 approval details
        Map<String, Object> level1 = new HashMap<>();
        level1.put("status", dispute.getLevel1ApprovalStatus().toString());
        level1.put("approver", dispute.getLevel1ApproverUsername());
        level1.put("notes", dispute.getLevel1ApprovalNotes());
        level1.put("timestamp", dispute.getLevel1ApprovalTimestamp());
        level1.put("escalated", dispute.isLevel1Escalated());
        response.put("level1", level1);
        
        // Level 2 approval details
        Map<String, Object> level2 = new HashMap<>();
        level2.put("status", dispute.getLevel2ApprovalStatus().toString());
        level2.put("approver", dispute.getLevel2ApproverUsername());
        level2.put("notes", dispute.getLevel2ApprovalNotes());
        level2.put("timestamp", dispute.getLevel2ApprovalTimestamp());
        level2.put("escalated", dispute.isLevel2Escalated());
        response.put("level2", level2);
        
        // Level 3 approval details
        Map<String, Object> level3 = new HashMap<>();
        level3.put("status", dispute.getLevel3ApprovalStatus().toString());
        level3.put("approver", dispute.getLevel3ApproverUsername());
        level3.put("notes", dispute.getLevel3ApprovalNotes());
        level3.put("timestamp", dispute.getLevel3ApprovalTimestamp());
        level3.put("escalated", dispute.isLevel3Escalated());
        response.put("level3", level3);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Submit a Level 1 approval decision
     */
    @PostMapping("/{caseId}/level1")
    public ResponseEntity<Map<String, String>> submitLevel1Decision(
            @PathVariable String caseId,
            @RequestParam ApprovalStatus decision,
            @RequestParam String notes,
            @RequestParam String username) {
        log.info("Processing Level 1 decision for dispute {}: {}", caseId, decision);
        
        Dispute dispute = approvalService.recordLevel1Decision(caseId, decision, notes, username);
        boolean success = dispute != null;
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("status", "success");
            response.put("message", "Level 1 decision recorded successfully");
        } else {
            response.put("status", "error");
            response.put("message", "Failed to record Level 1 decision");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Submit a Level 2 approval decision
     */
    @PostMapping("/{caseId}/level2")
    public ResponseEntity<Map<String, String>> submitLevel2Decision(
            @PathVariable String caseId,
            @RequestParam ApprovalStatus decision,
            @RequestParam String notes,
            @RequestParam String username) {
        log.info("Processing Level 2 decision for dispute {}: {}", caseId, decision);
        
        Dispute dispute = approvalService.recordLevel2Decision(caseId, decision, notes, username);
        boolean success = dispute != null;
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("status", "success");
            response.put("message", "Level 2 decision recorded successfully");
        } else {
            response.put("status", "error");
            response.put("message", "Failed to record Level 2 decision");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Submit a Level 3 approval decision
     */
    @PostMapping("/{caseId}/level3")
    public ResponseEntity<Map<String, String>> submitLevel3Decision(
            @PathVariable String caseId,
            @RequestParam ApprovalStatus decision,
            @RequestParam String notes,
            @RequestParam String username) {
        log.info("Processing Level 3 decision for dispute {}: {}", caseId, decision);
        
        Dispute dispute = approvalService.recordLevel3Decision(caseId, decision, notes, username);
        boolean success = dispute != null;
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("status", "success");
            response.put("message", "Level 3 decision recorded successfully");
        } else {
            response.put("status", "error");
            response.put("message", "Failed to record Level 3 decision");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if a dispute requires multi-level approval
     */
    @GetMapping("/{caseId}/requires-multi-level")
    public ResponseEntity<Map<String, Boolean>> requiresMultiLevelApproval(@PathVariable String caseId) {
        log.info("Checking if dispute requires multi-level approval: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Dispute dispute = disputeOpt.get();
        boolean requiresMultiApproval = approvalService.requiresMultiLevelApproval(dispute);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("requiresMultiLevelApproval", requiresMultiApproval);
        
        return ResponseEntity.ok(response);
    }
} 