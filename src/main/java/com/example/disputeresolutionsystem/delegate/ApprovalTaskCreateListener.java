package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Task listener that populates approval task form data
 */
@Slf4j
@Component("approvalTaskCreateListener")
@RequiredArgsConstructor
public class ApprovalTaskCreateListener implements TaskListener {

    private final DisputeRepository disputeRepository;

    @Override
    public void notify(DelegateTask delegateTask) {
        String caseId = (String) delegateTask.getVariable("caseId");
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        
        log.info("Setting up approval task form data for task: {}, case: {}", taskDefinitionKey, caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found for case ID: {}", caseId);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Common variables for all approval levels
        delegateTask.setVariable("submittedUserFullName", dispute.getSubmittedUserFullName());
        delegateTask.setVariable("submittedUserAddress", dispute.getSubmittedUserAddress());
        delegateTask.setVariable("submittedUserPhoneNumber", dispute.getSubmittedUserPhoneNumber());
        delegateTask.setVariable("submittedUserEmailAddress", dispute.getSubmittedUserEmailAddress());
        delegateTask.setVariable("disputeType", dispute.getDisputeType());
        delegateTask.setVariable("complexityLevel", dispute.getComplexityLevel().toString());
        delegateTask.setVariable("priorityLevel", dispute.getPriorityLevel().toString());
        delegateTask.setVariable("description", dispute.getDescription());
        delegateTask.setVariable("piiValidationStatus", dispute.getPiiValidationStatus().toString());
        
        // Level-specific variables
        switch (taskDefinitionKey) {
            case "task_level1_approval":
                // No previous approvals to include
                break;
                
            case "task_level2_approval":
                // Include Level 1 approval data
                delegateTask.setVariable("level1ApprovalStatus", dispute.getLevel1ApprovalStatus().toString());
                delegateTask.setVariable("level1ApproverUsername", dispute.getLevel1ApproverUsername());
                delegateTask.setVariable("level1ApprovalNotes", dispute.getLevel1ApprovalNotes());
                delegateTask.setVariable("level1ApprovalTimestamp", dispute.getLevel1ApprovalTimestamp());
                break;
                
            case "task_level3_approval":
                // Include Level 1 and 2 approval data
                delegateTask.setVariable("level1ApprovalStatus", dispute.getLevel1ApprovalStatus().toString());
                delegateTask.setVariable("level1ApproverUsername", dispute.getLevel1ApproverUsername());
                delegateTask.setVariable("level1ApprovalNotes", dispute.getLevel1ApprovalNotes());
                delegateTask.setVariable("level1ApprovalTimestamp", dispute.getLevel1ApprovalTimestamp());
                
                delegateTask.setVariable("level2ApprovalStatus", dispute.getLevel2ApprovalStatus().toString());
                delegateTask.setVariable("level2ApproverUsername", dispute.getLevel2ApproverUsername());
                delegateTask.setVariable("level2ApprovalNotes", dispute.getLevel2ApprovalNotes());
                delegateTask.setVariable("level2ApprovalTimestamp", dispute.getLevel2ApprovalTimestamp());
                break;
                
            default:
                log.warn("Unknown task definition key: {}", taskDefinitionKey);
        }
        
        log.info("Task form data set up for {}, case: {}", taskDefinitionKey, caseId);
    }
} 