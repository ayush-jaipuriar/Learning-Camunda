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
} 