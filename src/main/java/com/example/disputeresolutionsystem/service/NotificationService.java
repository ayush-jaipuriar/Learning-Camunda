package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;

public interface NotificationService {
    
    /**
     * Send a notification about a new dispute being assigned to a case officer
     * 
     * @param dispute The dispute that was assigned
     * @param officer The officer to whom the dispute was assigned
     */
    void sendAssignmentNotification(Dispute dispute, CaseOfficer officer);
    
    /**
     * Send a notification about a dispute approaching its SLA deadline
     * 
     * @param dispute The dispute that is approaching its deadline
     */
    void sendReminderNotification(Dispute dispute);
    
    /**
     * Send an escalation notification to a target group or supervisor
     * 
     * @param recipientId The ID of the recipient (group or individual)
     * @param subject The subject of the notification
     * @param message The message content
     */
    void sendEscalationNotification(String recipientId, String subject, String message);
    
    /**
     * Send a notification about a dispute being completed and ready for customer notification
     * 
     * @param dispute The completed dispute
     */
    void sendDisputeCompletionNotification(Dispute dispute);
    
    /**
     * Send a reminder to a case owner about approaching SLA deadline
     * @param dispute The dispute case
     * @param minutesRemaining Minutes remaining until SLA breach
     * @return true if the reminder was sent successfully
     */
    boolean sendSLAReminderNotification(Dispute dispute, int minutesRemaining);
    
    /**
     * Send an escalation notification to a supervisor
     * @param dispute The dispute that was escalated
     * @param supervisor The supervisor the case was escalated to
     * @return true if the notification was sent successfully
     */
    boolean sendEscalationNotification(Dispute dispute, CaseOfficer supervisor);
    
    /**
     * Send an SLA violation notification
     * @param dispute The dispute that breached SLA
     * @return true if the notification was sent successfully
     */
    boolean sendSLAViolationNotification(Dispute dispute);
    
    /**
     * Generate and send a compliance report for severe SLA violations
     * @param dispute The dispute with severe SLA violation
     * @return true if the report was generated and sent successfully
     */
    boolean generateComplianceReport(Dispute dispute);
} 