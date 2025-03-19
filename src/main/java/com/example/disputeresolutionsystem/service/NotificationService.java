package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;

public interface NotificationService {
    
    /**
     * Send a reminder to a case owner about approaching SLA deadline
     * @param dispute The dispute case
     * @param daysRemaining Days remaining until SLA breach
     * @return true if the reminder was sent successfully
     */
    boolean sendSLAReminderNotification(Dispute dispute, int daysRemaining);
    
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