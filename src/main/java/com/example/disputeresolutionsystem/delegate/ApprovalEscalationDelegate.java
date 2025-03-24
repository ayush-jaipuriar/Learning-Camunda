package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.service.MultiLevelApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Delegate that handles escalation of approval tasks when they timeout
 */
@Slf4j
@Component("approvalEscalationDelegate")
@RequiredArgsConstructor
public class ApprovalEscalationDelegate implements JavaDelegate {

    private final MultiLevelApprovalService approvalService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        Integer approvalLevel = (Integer) execution.getVariable("approvalLevel");
        
        if (approvalLevel == null) {
            log.error("No approval level specified for escalation of dispute: {}", caseId);
            execution.setVariable("escalationSuccessful", false);
            return;
        }
        
        log.info("Escalating Level {} approval for dispute: {}", approvalLevel, caseId);
        
        boolean escalated = approvalService.escalateApproval(caseId, approvalLevel);
        
        execution.setVariable("escalationSuccessful", escalated);
        
        if (escalated) {
            log.info("Successfully escalated Level {} approval for dispute: {}", approvalLevel, caseId);
        } else {
            log.warn("Failed to escalate Level {} approval for dispute: {}", approvalLevel, caseId);
        }
    }
} 