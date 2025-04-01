package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Task listener that processes manual approver assignments when completed
 */
@Slf4j
@Component("manualApproverAssignmentListener")
@RequiredArgsConstructor
public class ManualApproverAssignmentListener implements TaskListener {

    private final DisputeRepository disputeRepository;
    private final CaseOfficerRepository caseOfficerRepository;

    @Override
    public void notify(DelegateTask delegateTask) {
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String caseId = (String) delegateTask.getVariable("caseId");
        
        if (!"Activity_ManualAssignment".equals(taskDefinitionKey)) {
            return;
        }
        
        log.info("Processing manual approver assignment for dispute: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found for case ID: {}", caseId);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Get the assigned approvers from the form
        String level1ApproverUsername = (String) delegateTask.getVariable("level1ApproverUsername");
        String level2ApproverUsername = (String) delegateTask.getVariable("level2ApproverUsername");
        String level3ApproverUsername = (String) delegateTask.getVariable("level3ApproverUsername");
        
        log.info("Manual assignment received - Level 1: {}, Level 2: {}, Level 3: {}",
            level1ApproverUsername, level2ApproverUsername, level3ApproverUsername);
        
        // Keep track of which level was actually updated
        Integer highestUpdatedLevel = null;
        
        // Update the dispute with the manually assigned approvers
        if (level1ApproverUsername != null && !level1ApproverUsername.isEmpty()) {
            Optional<CaseOfficer> officer = caseOfficerRepository.findByUsername(level1ApproverUsername);
            if (officer.isPresent()) {
                dispute.setLevel1ApproverUsername(level1ApproverUsername);
                dispute.setLevel1ApprovalStatus(Dispute.ApprovalStatus.PENDING);
                log.info("Assigned Level 1 approver: {} for dispute: {}", level1ApproverUsername, caseId);
                
                // Set process variable for successful assignment
                delegateTask.setVariable("level1AssignmentSuccessful", true);
                highestUpdatedLevel = 1;
            } else {
                log.warn("Unable to find officer with username: {} for Level 1 approval", level1ApproverUsername);
            }
        }
        
        if (level2ApproverUsername != null && !level2ApproverUsername.isEmpty()) {
            Optional<CaseOfficer> officer = caseOfficerRepository.findByUsername(level2ApproverUsername);
            if (officer.isPresent()) {
                dispute.setLevel2ApproverUsername(level2ApproverUsername);
                dispute.setLevel2ApprovalStatus(Dispute.ApprovalStatus.PENDING);
                log.info("Assigned Level 2 approver: {} for dispute: {}", level2ApproverUsername, caseId);
                
                // Set process variable for successful assignment
                delegateTask.setVariable("level2AssignmentSuccessful", true);
                highestUpdatedLevel = 2;
            } else {
                log.warn("Unable to find officer with username: {} for Level 2 approval", level2ApproverUsername);
            }
        }
        
        if (level3ApproverUsername != null && !level3ApproverUsername.isEmpty()) {
            Optional<CaseOfficer> officer = caseOfficerRepository.findByUsername(level3ApproverUsername);
            if (officer.isPresent()) {
                dispute.setLevel3ApproverUsername(level3ApproverUsername);
                dispute.setLevel3ApprovalStatus(Dispute.ApprovalStatus.PENDING);
                log.info("Assigned Level 3 approver: {} for dispute: {}", level3ApproverUsername, caseId);
                
                // Set process variable for successful assignment
                delegateTask.setVariable("level3AssignmentSuccessful", true);
                highestUpdatedLevel = 3;
            } else {
                log.warn("Unable to find officer with username: {} for Level 3 approval", level3ApproverUsername);
            }
        }
        
        // Get the input parameter for failed assignment level
        Integer failedAssignmentLevel = null;
        Object failedLevelVar = delegateTask.getVariable("failedAssignmentLevel");
        if (failedLevelVar != null) {
            if (failedLevelVar instanceof Integer) {
                failedAssignmentLevel = (Integer) failedLevelVar;
            } else if (failedLevelVar instanceof String) {
                try {
                    failedAssignmentLevel = Integer.parseInt((String) failedLevelVar);
                } catch (NumberFormatException e) {
                    log.warn("Invalid failedAssignmentLevel format: {}", failedLevelVar);
                }
            }
        }
        
        log.info("Failed assignment level from input: {}, highest updated level: {}", 
                 failedAssignmentLevel, highestUpdatedLevel);
                 
        // If we have both a failedAssignmentLevel and highestUpdatedLevel,
        // use the highest of the two to determine where to route
        if (failedAssignmentLevel != null && highestUpdatedLevel != null) {
            delegateTask.setVariable("failedAssignmentLevel", Math.max(failedAssignmentLevel, highestUpdatedLevel));
        } else if (highestUpdatedLevel != null) {
            delegateTask.setVariable("failedAssignmentLevel", highestUpdatedLevel);
        } else if (failedAssignmentLevel != null) {
            delegateTask.setVariable("failedAssignmentLevel", failedAssignmentLevel);
        } else {
            // Default to level 1 if we couldn't determine
            delegateTask.setVariable("failedAssignmentLevel", 1);
        }
        
        // Save the updated dispute
        disputeRepository.save(dispute);
        log.info("Successfully processed manual approver assignment for dispute: {}", caseId);
    }
} 