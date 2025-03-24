package com.example.disputeresolutionsystem.config;

import com.example.disputeresolutionsystem.model.AuditLog;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    /**
     * Log when a dispute is created
     */
    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.service.DisputeService.createDispute(..))",
        returning = "dispute"
    )
    public void logDisputeCreation(JoinPoint joinPoint, Dispute dispute) {
        log.debug("Logging dispute creation: {}", dispute.getCaseId());
        
        auditService.logAction(
            AuditLog.ActionType.SUBMISSION,
            dispute,
            dispute.getUserId(),
            "Dispute submitted",
            null,
            dispute.getStatus(),
            null
        );
    }
    
    /**
     * Log when a dispute is assigned to an officer
     */
    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.service.CaseAssignmentService.assignDisputeToOfficer(..))",
        returning = "result"
    )
    public void logDisputeAssignment(JoinPoint joinPoint, Object result) {
        if (joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof Dispute) {
            Dispute dispute = (Dispute) joinPoint.getArgs()[0];
            
            Map<String, Object> actionData = new HashMap<>();
            if (dispute.getAssignedOfficer() != null) {
                actionData.put("officerId", dispute.getAssignedOfficer().getId());
                actionData.put("officerUsername", dispute.getAssignedOfficer().getUsername());
            }
            
            auditService.logAction(
                AuditLog.ActionType.ASSIGNMENT,
                dispute,
                "system",
                "Dispute assigned to officer",
                null,
                dispute.getStatus(),
                actionData
            );
        }
    }
    
    /**
     * Log when a dispute is escalated
     */
    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.service.CaseAssignmentService.escalateDispute(..))",
        returning = "result"
    )
    public void logDisputeEscalation(JoinPoint joinPoint, boolean result) {
        if (result && joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof Dispute) {
            Dispute dispute = (Dispute) joinPoint.getArgs()[0];
            
            auditService.logAction(
                AuditLog.ActionType.ESCALATION,
                dispute,
                "system",
                "Dispute escalated due to SLA violation or manual trigger",
                null,
                dispute.getStatus(),
                null
            );
        }
    }
    
    /**
     * Log when an SLA violation notification is sent
     */
    @After("execution(* com.example.disputeresolutionsystem.service.NotificationService.sendSLAViolationNotification(..))")
    public void logSLAViolation(JoinPoint joinPoint) {
        if (joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof Dispute) {
            Dispute dispute = (Dispute) joinPoint.getArgs()[0];
            
            auditService.logAction(
                AuditLog.ActionType.SLA_VIOLATION,
                dispute,
                "system",
                "SLA violation detected and notification sent",
                null,
                dispute.getStatus(),
                null
            );
        }
    }
    
    /**
     * Log when a compliance report is generated
     */
    @After("execution(* com.example.disputeresolutionsystem.service.NotificationService.generateComplianceReport(..))")
    public void logComplianceReportGeneration(JoinPoint joinPoint) {
        if (joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof Dispute) {
            Dispute dispute = (Dispute) joinPoint.getArgs()[0];
            
            auditService.logAction(
                AuditLog.ActionType.COMPLIANCE_REPORT_GENERATED,
                dispute,
                "system",
                "Compliance report generated for severe SLA violation",
                null,
                dispute.getStatus(),
                null
            );
        }
    }

    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.repository.DisputeRepository.save(..))",
        returning = "result"
    )
    public void logDisputeChanges(JoinPoint joinPoint, Object result) {
        if (!(result instanceof Dispute)) {
            return;
        }
        
        Dispute dispute = (Dispute) result;
        
        // Log dispute creation or update
        log.debug("Logging dispute creation: {}", dispute.getCaseId());
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("timestamp", LocalDateTime.now());
        actionData.put("caseId", dispute.getCaseId());
        actionData.put("userId", dispute.getUserId());
        actionData.put("actionType", "DISPUTE_UPDATE");
        actionData.put("newStatus", dispute.getStatus());
        
        logAuditEvent(actionData);
    }
    
    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.service.DisputeService.assignDispute(..))",
        returning = "result"
    )
    public void logDisputeAssignmentService(JoinPoint joinPoint, Object result) {
        if (result == null) return;
        
        Map<String, Object> actionData = new HashMap<>();
        Dispute dispute = (Dispute) result;
        
        actionData.put("caseId", dispute.getCaseId());
        
        // Include officer information if available
        if (dispute.getAssignedOfficer() != null) {
            actionData.put("officerId", dispute.getAssignedOfficer().getId());
            actionData.put("officerUsername", dispute.getAssignedOfficer().getUsername());
        }
        
        actionData.put("timestamp", LocalDateTime.now().toString());
        actionData.put("action", "CASE_ASSIGNMENT");
        
        logAuditEvent(actionData);
    }
    
    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.service.DisputeService.validatePII(..))",
        returning = "result"
    )
    public void logPIIValidation(JoinPoint joinPoint, Object result) {
        if (!(result instanceof Dispute)) {
            return;
        }
        
        Dispute dispute = (Dispute) result;
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("timestamp", LocalDateTime.now());
        actionData.put("caseId", dispute.getCaseId());
        actionData.put("actionType", "PII_VALIDATION");
        actionData.put("status", dispute.getStatus());
        actionData.put("validationResult", dispute.getPiiValidationStatus());
        
        logAuditEvent(actionData);
    }
    
    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.service.MultiLevelApprovalService.recordLevel1Decision(..))",
        returning = "result"
    )
    public void logLevel1Decision(JoinPoint joinPoint, Object result) {
        if (!(result instanceof Dispute)) {
            return;
        }
        
        Dispute dispute = (Dispute) result;
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("timestamp", LocalDateTime.now());
        actionData.put("caseId", dispute.getCaseId());
        actionData.put("actionType", "LEVEL1_DECISION");
        actionData.put("status", dispute.getStatus());
        actionData.put("level1Status", dispute.getLevel1ApprovalStatus());
        actionData.put("level1Approver", dispute.getLevel1ApproverUsername());
        
        logAuditEvent(actionData);
    }
    
    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.service.MultiLevelApprovalService.recordLevel2Decision(..))",
        returning = "result"
    )
    public void logLevel2Decision(JoinPoint joinPoint, Object result) {
        if (!(result instanceof Dispute)) {
            return;
        }
        
        Dispute dispute = (Dispute) result;
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("timestamp", LocalDateTime.now());
        actionData.put("caseId", dispute.getCaseId());
        actionData.put("actionType", "LEVEL2_DECISION");
        actionData.put("status", dispute.getStatus());
        actionData.put("level2Status", dispute.getLevel2ApprovalStatus());
        actionData.put("level2Approver", dispute.getLevel2ApproverUsername());
        
        logAuditEvent(actionData);
    }
    
    @AfterReturning(
        pointcut = "execution(* com.example.disputeresolutionsystem.service.MultiLevelApprovalService.recordLevel3Decision(..))",
        returning = "result"
    )
    public void logLevel3Decision(JoinPoint joinPoint, Object result) {
        if (!(result instanceof Dispute)) {
            return;
        }
        
        Dispute dispute = (Dispute) result;
        
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("timestamp", LocalDateTime.now());
        actionData.put("caseId", dispute.getCaseId());
        actionData.put("actionType", "LEVEL3_DECISION");
        actionData.put("status", dispute.getStatus());
        actionData.put("level3Status", dispute.getLevel3ApprovalStatus());
        actionData.put("level3Approver", dispute.getLevel3ApproverUsername());
        
        logAuditEvent(actionData);
    }
    
    private void logAuditEvent(Map<String, Object> actionData) {
        // In a real application, this would write to a database or dedicated audit log
        // For now, just log it
        log.info("AUDIT: {}", actionData);
    }
} 