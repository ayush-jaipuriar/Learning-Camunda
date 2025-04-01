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

import java.util.Optional;

/**
 * Delegate that handles "need more information" requests from any approval level
 */
@Slf4j
@Component("requestMoreInfoDelegate")
@RequiredArgsConstructor
public class RequestMoreInfoDelegate implements JavaDelegate {

    private final DisputeRepository disputeRepository;
    private final NotificationService notificationService;
    private final ProcessMessageServiceImpl processMessageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        String approvalNotes = (String) execution.getVariable("approvalNotes");
        String taskDefinitionKey = execution.getCurrentActivityId();
        log.info("Processing 'need more information' request for dispute: {} at {}", caseId, taskDefinitionKey);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.error("Dispute not found: {}", caseId);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Set the status based on which level requested more information
        if (taskDefinitionKey.toLowerCase().contains("level1")) {
            dispute.setStatus("Level 1 Needs More Information");
        } else if (taskDefinitionKey.toLowerCase().contains("level2")) {
            dispute.setStatus("Level 2 Needs More Information");
        } else if (taskDefinitionKey.toLowerCase().contains("level3")) {
            dispute.setStatus("Level 3 Needs More Information");
        } else {
            dispute.setStatus("Needs More Information");
        }
        
        // Store the requestor level and notes for reference
        String requestorLevel = determineRequestorLevel(taskDefinitionKey);
        String requestDetails = String.format(
                "Additional Information Requested by %s: %s", 
                requestorLevel,
                approvalNotes != null ? approvalNotes : "No details provided");
        
        // Add request details to the dispute
        if (dispute.getDescription() == null) {
            dispute.setDescription(requestDetails);
        } else {
            dispute.setDescription(dispute.getDescription() + "\n\n" + requestDetails);
        }
        
        // Save the updated dispute
        disputeRepository.save(dispute);
        
        // Notify the user about the additional information request
        sendMoreInfoNotification(dispute, requestorLevel, approvalNotes);
        
        // Send message to the main process
        try {
            processMessageService.sendMultiLevelApprovalComplete(caseId, "NEEDS_MORE_INFO");
            log.info("More info request completion message sent for dispute: {}", caseId);
        } catch (Exception e) {
            log.error("Failed to send more info request completion message for dispute: {}", caseId, e);
        }
        
        log.info("More information request processed for dispute {}", caseId);
    }
    
    /**
     * Determine the requestor level based on the task definition key
     */
    private String determineRequestorLevel(String taskDefinitionKey) {
        if (taskDefinitionKey.toLowerCase().contains("level1")) {
            return "Level 1 Reviewer";
        } else if (taskDefinitionKey.toLowerCase().contains("level2")) {
            return "Level 2 Reviewer (Senior Officer)";
        } else if (taskDefinitionKey.toLowerCase().contains("level3")) {
            return "Level 3 Reviewer (Compliance Team)";
        } else {
            return "Reviewer";
        }
    }
    
    /**
     * Send a notification to the user about the additional information request
     */
    private void sendMoreInfoNotification(Dispute dispute, String requestorLevel, String notes) {
        try {
            log.info("Sending 'more information needed' notification to user {} for dispute {}", 
                    dispute.getUserId(), dispute.getCaseId());
            
            // In a real system, we would have a special notification for this
            // For now, we'll use the standard completion notification
            notificationService.sendDisputeCompletionNotification(dispute);
            
            // Log the details that would be sent
            log.info("More Info Request from {}: {}", requestorLevel, notes);
        } catch (Exception e) {
            log.error("Failed to send 'more information needed' notification for dispute: {}", 
                    dispute.getCaseId(), e);
        }
    }
} 