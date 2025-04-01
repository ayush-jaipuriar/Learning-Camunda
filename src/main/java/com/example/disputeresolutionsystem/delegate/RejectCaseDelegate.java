package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.NotificationService;
import com.example.disputeresolutionsystem.service.impl.ProcessMessageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Delegate that handles case rejection from any approval level
 */
@Slf4j
@Component("rejectCaseDelegate")
@RequiredArgsConstructor
public class RejectCaseDelegate implements JavaDelegate {

    private final DisputeRepository disputeRepository;
    private final NotificationService notificationService;
    private final ProcessMessageServiceImpl processMessageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        String rejectionLevel = (String) execution.getVariable("approvalDecision");
        String taskDefinitionKey = execution.getCurrentActivityId();
        log.info("Processing rejection for dispute: {} at {}", caseId, taskDefinitionKey);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.error("Dispute not found: {}", caseId);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Set the rejection status based on where it was rejected
        if (taskDefinitionKey.toLowerCase().contains("level1")) {
            dispute.setStatus("Rejected at Level 1");
        } else if (taskDefinitionKey.toLowerCase().contains("level2")) {
            dispute.setStatus("Rejected at Level 2");
        } else if (taskDefinitionKey.toLowerCase().contains("level3")) {
            dispute.setStatus("Rejected at Level 3");
        } else {
            dispute.setStatus("Rejected");
        }
        
        // Mark the case as resolved
        dispute.setResolutionTimestamp(LocalDateTime.now());
        
        // Record rejection summary
        StringBuilder rejectionSummary = new StringBuilder();
        rejectionSummary.append("Dispute REJECTED in multi-level review process.\n");
        
        if (dispute.getLevel1ApprovalStatus() != null) {
            rejectionSummary.append("Level 1: ").append(dispute.getLevel1ApprovalStatus())
                    .append(" by ").append(dispute.getLevel1ApproverUsername())
                    .append(" - Notes: ").append(dispute.getLevel1ApprovalNotes()).append("\n");
        }
        
        if (dispute.getLevel2ApprovalStatus() != null && 
                dispute.getLevel2ApprovalStatus() != Dispute.ApprovalStatus.PENDING) {
            rejectionSummary.append("Level 2: ").append(dispute.getLevel2ApprovalStatus())
                    .append(" by ").append(dispute.getLevel2ApproverUsername())
                    .append(" - Notes: ").append(dispute.getLevel2ApprovalNotes()).append("\n");
        }
        
        if (dispute.getLevel3ApprovalStatus() != null && 
                dispute.getLevel3ApprovalStatus() != Dispute.ApprovalStatus.PENDING) {
            rejectionSummary.append("Level 3: ").append(dispute.getLevel3ApprovalStatus())
                    .append(" by ").append(dispute.getLevel3ApproverUsername())
                    .append(" - Notes: ").append(dispute.getLevel3ApprovalNotes()).append("\n");
        }
        
        // Update the dispute description with rejection details
        dispute.setDescription(dispute.getDescription() + "\n\n" + rejectionSummary.toString());
        
        // Save the updated dispute
        disputeRepository.save(dispute);
        
        // Send rejection notification to the user
        sendRejectionNotification(dispute);
        
        // Send message to the main process
        try {
            processMessageService.sendMultiLevelApprovalComplete(caseId, "REJECTED");
            log.info("Rejection completion message sent for dispute: {}", caseId);
        } catch (Exception e) {
            log.error("Failed to send rejection completion message for dispute: {}", caseId, e);
        }
        
        log.info("Dispute {} has been rejected and closed", caseId);
    }
    
    /**
     * Send a notification to the user about the rejection
     */
    private void sendRejectionNotification(Dispute dispute) {
        try {
            // In a real implementation, we would call a method in the NotificationService
            // For now, we'll just log it
            log.info("Sending rejection notification to user {} for dispute {}", 
                    dispute.getUserId(), dispute.getCaseId());
            
            notificationService.sendDisputeCompletionNotification(dispute);
        } catch (Exception e) {
            log.error("Failed to send rejection notification for dispute: {}", dispute.getCaseId(), e);
        }
    }
} 