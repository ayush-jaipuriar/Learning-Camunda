package com.example.disputeresolutionsystem.service.impl;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationServiceImpl implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    
    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @Override
    public boolean sendSLAReminderNotification(Dispute dispute, int minutesRemaining) {
        CaseOfficer assignedOfficer = dispute.getAssignedOfficer();
        if (assignedOfficer == null) {
            logger.warn("Cannot send SLA reminder for dispute {} - no assigned officer", dispute.getCaseId());
            return false;
        }
        
        // In a real implementation, this would send an email or other notification
        // For now, we'll just log it
        logger.info("REMINDER: SLA deadline approaching for dispute {}", dispute.getCaseId());
        logger.info("  - Assigned to: {} ({})", assignedOfficer.getUsername(), assignedOfficer.getEmail());
        logger.info("  - Time remaining: {} minutes", minutesRemaining);
        logger.info("  - SLA Deadline: {}", DATE_FORMATTER.format(dispute.getSlaDeadline()));
        
        // Update reminder count in the dispute
        dispute.setRemindersSent(dispute.getRemindersSent() + 1);
        
        return true;
    }

    @Override
    public boolean sendEscalationNotification(Dispute dispute, CaseOfficer supervisor) {
        if (supervisor == null) {
            logger.warn("Cannot send escalation notification - no supervisor provided for dispute {}", dispute.getCaseId());
            return false;
        }
        
        // In a real implementation, this would send an email or other notification
        // For now, we'll just log it
        logger.info("ESCALATION: Dispute {} has been escalated", dispute.getCaseId());
        logger.info("  - Escalated to: {} ({})", supervisor.getUsername(), supervisor.getEmail());
        logger.info("  - Original SLA Deadline: {}", DATE_FORMATTER.format(dispute.getSlaDeadline()));
        logger.info("  - Escalation Time: {}", DATE_FORMATTER.format(LocalDateTime.now()));
        
        return true;
    }

    @Override
    public boolean sendSLAViolationNotification(Dispute dispute) {
        // In a real implementation, this would send an email or other notification
        // For now, we'll just log it
        logger.info("SLA VIOLATION: Dispute {} has breached SLA", dispute.getCaseId());
        logger.info("  - SLA Deadline: {}", DATE_FORMATTER.format(dispute.getSlaDeadline()));
        logger.info("  - Current Time: {}", DATE_FORMATTER.format(LocalDateTime.now()));
        
        if (dispute.getAssignedOfficer() != null) {
            logger.info("  - Assigned to: {} ({})", 
                    dispute.getAssignedOfficer().getUsername(), 
                    dispute.getAssignedOfficer().getEmail());
        } else {
            logger.info("  - Not assigned to any officer");
        }
        
        return true;
    }

    @Override
    public boolean generateComplianceReport(Dispute dispute) {
        // In a real implementation, this would generate a report and potentially send it
        // For now, we'll just log it
        logger.info("COMPLIANCE REPORT GENERATED: For severely delayed dispute {}", dispute.getCaseId());
        logger.info("  - SLA Deadline: {}", DATE_FORMATTER.format(dispute.getSlaDeadline()));
        logger.info("  - Current Delay: {} minutes", 
                java.time.Duration.between(dispute.getSlaDeadline(), LocalDateTime.now()).toMinutes());
        logger.info("  - Status: {}", dispute.getStatus());
        logger.info("  - Reminders Sent: {}", dispute.getRemindersSent());
        
        if (dispute.getAssignedOfficer() != null) {
            logger.info("  - Assigned to: {} ({})", 
                    dispute.getAssignedOfficer().getUsername(), 
                    dispute.getAssignedOfficer().getEmail());
        }
        
        // Mark the dispute as having a compliance report
        dispute.setComplianceReportGenerated(true);
        
        return true;
    }

    @Override
    public void sendAssignmentNotification(Dispute dispute, CaseOfficer officer) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled. Would have sent assignment notification for dispute {} to {}", 
                dispute.getCaseId(), officer.getUsername());
            return;
        }
        
        // In a real system, this would send an email, SMS, or other notification
        logger.info("Sending assignment notification for dispute {} to {}", 
            dispute.getCaseId(), officer.getUsername());
        
        // Simulate sending notification
        String message = String.format(
            "Hello %s, a new dispute (ID: %s) has been assigned to you. Please review it as soon as possible.",
            officer.getFullName(), dispute.getCaseId());
        
        // Log the notification for demo purposes
        logger.info("Assignment notification sent: {}", message);
    }
    
    @Override
    public void sendReminderNotification(Dispute dispute) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled. Would have sent reminder for dispute {}", 
                dispute.getCaseId());
            return;
        }
        
        CaseOfficer officer = dispute.getAssignedOfficer();
        if (officer == null) {
            logger.warn("Cannot send reminder for dispute {} - no assigned officer", dispute.getCaseId());
            return;
        }
        
        // In a real system, this would send an email, SMS, or other notification
        logger.info("Sending reminder notification for dispute {} to {}", 
            dispute.getCaseId(), officer.getUsername());
        
        // Simulate sending notification
        String message = String.format(
            "REMINDER: Dispute ID %s is approaching its SLA deadline. Please complete your review as soon as possible.",
            dispute.getCaseId());
        
        // Log the notification for demo purposes
        logger.info("Reminder notification sent to {}: {}", officer.getUsername(), message);
    }
    
    @Override
    public void sendEscalationNotification(String recipientId, String subject, String message) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled. Would have sent escalation to {}: {}", recipientId, subject);
            return;
        }
        
        logger.info("Sending escalation notification to {} - Subject: {}", recipientId, subject);
        
        // Log the notification for demo purposes
        logger.info("Escalation notification sent to {}: {} - {}", recipientId, subject, message);
    }
    
    @Override
    public void sendDisputeCompletionNotification(Dispute dispute) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled. Would have sent completion notification for dispute {}", 
                dispute.getCaseId());
            return;
        }
        
        // In a real system, this would send an email, SMS, or other notification
        logger.info("Sending dispute completion notification for dispute {}", dispute.getCaseId());
        
        // Simulate sending notification
        String message = String.format(
            "Your dispute (ID: %s) has been processed. Final status: %s",
            dispute.getCaseId(), dispute.getStatus());
        
        // Log the notification for demo purposes
        logger.info("Dispute completion notification prepared for user {}: {}", 
            dispute.getUserId(), message);
    }

    @Override
    public boolean sendApprovalNotification(Dispute dispute) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled. Would have sent approval notification for dispute {}", 
                dispute.getCaseId());
            return false;
        }
        
        // In a real system, this would send an email, SMS, or other notification
        logger.info("Sending approval notification for dispute {}", dispute.getCaseId());
        
        StringBuilder message = new StringBuilder();
        message.append(String.format(
            "Your dispute (ID: %s) has been APPROVED through our multi-level review process.\n",
            dispute.getCaseId()));
        message.append("The dispute was reviewed and approved by:\n");
        message.append(String.format("Level 1 Approver: %s\n", dispute.getLevel1ApproverUsername()));
        message.append(String.format("Level 2 Approver: %s\n", dispute.getLevel2ApproverUsername()));
        message.append(String.format("Level 3 (Final) Approver: %s\n", dispute.getLevel3ApproverUsername()));
        message.append("\nThank you for your patience during our thorough review process.");
        
        // Log the notification for demo purposes
        logger.info("Approval notification prepared for user {}: {}", 
            dispute.getUserId(), message.toString());
        
        return true;
    }
} 