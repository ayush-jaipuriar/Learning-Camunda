package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.model.AuditLog;
import com.example.disputeresolutionsystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Get audit history for a specific dispute
     */
    @GetMapping("/disputes/{caseId}")
    public ResponseEntity<List<AuditLog>> getDisputeAuditHistory(@PathVariable String caseId) {
        log.info("Retrieving audit history for dispute: {}", caseId);
        List<AuditLog> auditLogs = auditService.getDisputeAuditHistory(caseId);
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * Get audit logs for a specific action type within a date range
     */
    @GetMapping("/actions/{actionType}")
    public ResponseEntity<List<AuditLog>> getActionAuditLogs(
            @PathVariable String actionType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            AuditLog.ActionType type = AuditLog.ActionType.valueOf(actionType.toUpperCase());
            log.info("Retrieving audit logs for action {} between {} and {}", 
                    type, startDate, endDate);
            
            List<AuditLog> auditLogs = auditService.getActionAuditLogs(type, startDate, endDate);
            return ResponseEntity.ok(auditLogs);
        } catch (IllegalArgumentException e) {
            log.error("Invalid action type: {}", actionType);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get audit logs for a specific user within a date range
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<AuditLog>> getUserAuditLogs(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Retrieving audit logs for user {} between {} and {}", 
                userId, startDate, endDate);
        
        List<AuditLog> auditLogs = auditService.getUserAuditLogs(userId, startDate, endDate);
        return ResponseEntity.ok(auditLogs);
    }
    
    /**
     * Get SLA violation statistics
     */
    @GetMapping("/violations/stats")
    public ResponseEntity<Map<String, Object>> getSlaViolationStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Retrieving SLA violation statistics between {} and {}", startDate, endDate);
        
        long violationCount = auditService.getSLAViolationCount(startDate, endDate);
        
        return ResponseEntity.ok(Map.of(
            "startDate", startDate.toString(),
            "endDate", endDate.toString(),
            "violationCount", violationCount
        ));
    }
    
    /**
     * Get all available action types
     */
    @GetMapping("/action-types")
    public ResponseEntity<AuditLog.ActionType[]> getActionTypes() {
        return ResponseEntity.ok(AuditLog.ActionType.values());
    }
} 