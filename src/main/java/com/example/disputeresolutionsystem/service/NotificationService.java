package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;
    
    /**
     * Send a notification about a new dispute being assigned to a case officer
     * 
     * @param dispute The dispute that was assigned
     * @param officer The officer to whom the dispute was assigned
     */
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
    
    /**
     * Send a notification about a dispute approaching its SLA deadline
     * 
     * @param dispute The dispute that is approaching its deadline
     */
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
    
    /**
     * Send an escalation notification to a target group or supervisor
     * 
     * @param recipientId The ID of the recipient (group or individual)
     * @param subject The subject of the notification
     * @param message The message content
     */
    public void sendEscalationNotification(String recipientId, String subject, String message) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled. Would have sent escalation to {}: {}", recipientId, subject);
            return;
        }
        
        logger.info("Sending escalation notification to {} - Subject: {}", recipientId, subject);
        
        // Log the notification for demo purposes
        logger.info("Escalation notification sent to {}: {} - {}", recipientId, subject, message);
    }
    
    /**
     * Send a notification about a dispute being completed and ready for customer notification
     * 
     * @param dispute The completed dispute
     */
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
} 