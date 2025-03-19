package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;

public interface CaseAssignmentService {
    
    /**
     * Assigns a dispute to an appropriate case officer based on complexity and priority
     * @param dispute The dispute to be assigned
     * @return The assigned case officer or null if no assignment could be made
     */
    CaseOfficer assignDisputeToOfficer(Dispute dispute);
    
    /**
     * Checks for disputes that have been unassigned for too long and escalates them
     */
    void checkForEscalations();
    
    /**
     * Escalates a dispute to a higher level
     * @param dispute The dispute to escalate
     * @return true if escalation was successful, false otherwise
     */
    boolean escalateDispute(Dispute dispute);
    
    /**
     * Checks for disputes approaching or exceeding their SLA deadlines and takes appropriate action
     * - For approaching deadlines: sends reminders
     * - For exceeded deadlines: escalates and sends violation notifications
     * - For severely exceeded deadlines: generates compliance reports
     */
    void monitorSLAViolations();
} 