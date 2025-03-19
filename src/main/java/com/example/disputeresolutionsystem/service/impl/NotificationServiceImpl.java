package com.example.disputeresolutionsystem.service.impl;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Override
    public boolean sendSLAReminderNotification(Dispute dispute, int minutesRemaining) {
        CaseOfficer assignedOfficer = dispute.getAssignedOfficer();
        if (assignedOfficer == null) {
            log.warn("Cannot send SLA reminder for dispute {} - no assigned officer", dispute.getCaseId());
            return false;
        }
        
        // In a real implementation, this would send an email or other notification
        // For now, we'll just log it
        log.info("REMINDER: SLA deadline approaching for dispute {}", dispute.getCaseId());
        log.info("  - Assigned to: {} ({})", assignedOfficer.getUsername(), assignedOfficer.getEmail());
        log.info("  - Time remaining: {} minutes", minutesRemaining);
        log.info("  - SLA Deadline: {}", DATE_FORMATTER.format(dispute.getSlaDeadline()));
        
        // Update reminder count in the dispute
        dispute.setRemindersSent(dispute.getRemindersSent() + 1);
        
        return true;
    }

    @Override
    public boolean sendEscalationNotification(Dispute dispute, CaseOfficer supervisor) {
        if (supervisor == null) {
            log.warn("Cannot send escalation notification - no supervisor provided for dispute {}", dispute.getCaseId());
            return false;
        }
        
        // In a real implementation, this would send an email or other notification
        // For now, we'll just log it
        log.info("ESCALATION: Dispute {} has been escalated", dispute.getCaseId());
        log.info("  - Escalated to: {} ({})", supervisor.getUsername(), supervisor.getEmail());
        log.info("  - Original SLA Deadline: {}", DATE_FORMATTER.format(dispute.getSlaDeadline()));
        log.info("  - Escalation Time: {}", DATE_FORMATTER.format(LocalDateTime.now()));
        
        return true;
    }

    @Override
    public boolean sendSLAViolationNotification(Dispute dispute) {
        // In a real implementation, this would send an email or other notification
        // For now, we'll just log it
        log.info("SLA VIOLATION: Dispute {} has breached SLA", dispute.getCaseId());
        log.info("  - SLA Deadline: {}", DATE_FORMATTER.format(dispute.getSlaDeadline()));
        log.info("  - Current Time: {}", DATE_FORMATTER.format(LocalDateTime.now()));
        
        if (dispute.getAssignedOfficer() != null) {
            log.info("  - Assigned to: {} ({})", 
                    dispute.getAssignedOfficer().getUsername(), 
                    dispute.getAssignedOfficer().getEmail());
        } else {
            log.info("  - Not assigned to any officer");
        }
        
        return true;
    }

    @Override
    public boolean generateComplianceReport(Dispute dispute) {
        // In a real implementation, this would generate a report and potentially send it
        // For now, we'll just log it
        log.info("COMPLIANCE REPORT GENERATED: For severely delayed dispute {}", dispute.getCaseId());
        log.info("  - SLA Deadline: {}", DATE_FORMATTER.format(dispute.getSlaDeadline()));
        log.info("  - Current Delay: {} minutes", 
                java.time.Duration.between(dispute.getSlaDeadline(), LocalDateTime.now()).toMinutes());
        log.info("  - Status: {}", dispute.getStatus());
        log.info("  - Reminders Sent: {}", dispute.getRemindersSent());
        
        if (dispute.getAssignedOfficer() != null) {
            log.info("  - Assigned to: {} ({})", 
                    dispute.getAssignedOfficer().getUsername(), 
                    dispute.getAssignedOfficer().getEmail());
        }
        
        // Mark the dispute as having a compliance report
        dispute.setComplianceReportGenerated(true);
        
        return true;
    }
} 