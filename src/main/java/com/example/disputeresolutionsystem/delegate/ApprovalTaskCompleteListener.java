package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute.ApprovalStatus;
import com.example.disputeresolutionsystem.service.MultiLevelApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * Task listener that processes approval decisions when tasks are completed
 */
@Slf4j
@Component("approvalTaskCompleteListener")
@RequiredArgsConstructor
public class ApprovalTaskCompleteListener implements TaskListener {

    private final MultiLevelApprovalService approvalService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String caseId = (String) delegateTask.getVariable("caseId");
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String approvalDecision = (String) delegateTask.getVariable("approvalDecision");
        String notes = (String) delegateTask.getVariable("approvalNotes");
        String username = delegateTask.getAssignee();
        
        if (approvalDecision == null) {
            log.warn("No approval decision provided for task: {}, case: {}", taskDefinitionKey, caseId);
            return;
        }
        
        ApprovalStatus decision = ApprovalStatus.valueOf(approvalDecision);
        log.info("Processing {} decision for task: {}, case: {}", decision, taskDefinitionKey, caseId);
        
        boolean success = false;
        
        switch (taskDefinitionKey) {
            case "task_level1_approval":
                success = approvalService.recordLevel1Decision(caseId, decision, notes, username);
                delegateTask.setVariable("level1Approved", decision == ApprovalStatus.APPROVED);
                break;
                
            case "task_level2_approval":
                success = approvalService.recordLevel2Decision(caseId, decision, notes, username);
                delegateTask.setVariable("level2Approved", decision == ApprovalStatus.APPROVED);
                break;
                
            case "task_level3_approval":
                success = approvalService.recordLevel3Decision(caseId, decision, notes, username);
                delegateTask.setVariable("level3Approved", decision == ApprovalStatus.APPROVED);
                break;
                
            default:
                log.warn("Unknown task definition key for approval: {}", taskDefinitionKey);
        }
        
        if (success) {
            log.info("Successfully processed {} decision for task: {}, case: {}", 
                    decision, taskDefinitionKey, caseId);
        } else {
            log.warn("Failed to process {} decision for task: {}, case: {}", 
                    decision, taskDefinitionKey, caseId);
        }
    }
} 