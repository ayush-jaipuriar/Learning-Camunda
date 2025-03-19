package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.AuditLog;
import com.example.disputeresolutionsystem.model.Dispute;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AuditService {

    /**
     * Log an action related to a dispute
     * 
     * @param actionType The type of action being performed
     * @param dispute The dispute being acted upon
     * @param userId The ID of the user performing the action
     * @param description A description of the action
     * @param previousStatus The previous status of the dispute (if applicable)
     * @param newStatus The new status of the dispute (if applicable)
     * @param actionData Additional data related to the action (will be stored as JSON)
     * @return The created audit log entry
     */
    AuditLog logAction(AuditLog.ActionType actionType, Dispute dispute, String userId, 
                        String description, String previousStatus, String newStatus, 
                        Map<String, Object> actionData);
    
    /**
     * Get the audit history for a specific dispute
     * 
     * @param caseId The case ID of the dispute
     * @return A list of audit log entries for the dispute, ordered by timestamp (newest first)
     */
    List<AuditLog> getDisputeAuditHistory(String caseId);
    
    /**
     * Get all audit logs for a specific action type within a date range
     * 
     * @param actionType The type of action to filter by
     * @param start The start of the date range
     * @param end The end of the date range
     * @return A list of matching audit log entries
     */
    List<AuditLog> getActionAuditLogs(AuditLog.ActionType actionType, LocalDateTime start, LocalDateTime end);
    
    /**
     * Get all audit logs for a specific user within a date range
     * 
     * @param userId The ID of the user
     * @param start The start of the date range
     * @param end The end of the date range
     * @return A list of matching audit log entries
     */
    List<AuditLog> getUserAuditLogs(String userId, LocalDateTime start, LocalDateTime end);
    
    /**
     * Get the count of SLA violations within a date range
     * 
     * @param start The start of the date range
     * @param end The end of the date range
     * @return The count of SLA violations
     */
    long getSLAViolationCount(LocalDateTime start, LocalDateTime end);
} 