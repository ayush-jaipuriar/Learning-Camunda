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
 * Delegate that finalizes an approval after all levels have approved
 */
@Slf4j
@Component("finalApprovalDelegate")
@RequiredArgsConstructor
public class FinalApprovalDelegate implements JavaDelegate {

    private final DisputeRepository disputeRepository;
    private final NotificationService notificationService;
    private final ProcessMessageServiceImpl processMessageService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        log.info("Finalizing approval for dispute: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.error("Dispute not found: {}", caseId);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Update dispute status
        dispute.setStatus("Approved");
        dispute.setResolutionTimestamp(LocalDateTime.now());
        
        // Record the final approval details
        StringBuilder approvalSummary = new StringBuilder();
        approvalSummary.append("Approved through multi-level review process.\n");
        approvalSummary.append("Level 1: ").append(dispute.getLevel1ApproverUsername())
                .append(" (").append(dispute.getLevel1ApprovalTimestamp()).append(")\n");
        approvalSummary.append("Level 2: ").append(dispute.getLevel2ApproverUsername())
                .append(" (").append(dispute.getLevel2ApprovalTimestamp()).append(")\n");
        approvalSummary.append("Level 3: ").append(dispute.getLevel3ApproverUsername())
                .append(" (").append(dispute.getLevel3ApprovalTimestamp()).append(")\n");
        
        // Record the approval details
        dispute.setDescription(dispute.getDescription() + "\n\n" + approvalSummary.toString());
        
        // Mark the case as resolved
        disputeRepository.save(dispute);
        
        // Notify the user of the approval
        try {
            notificationService.sendApprovalNotification(dispute);
            log.info("Approval notification sent for dispute: {}", caseId);
        } catch (Exception e) {
            log.error("Failed to send approval notification for dispute: {}", caseId, e);
        }
        
        // Send message to the main process
        try {
            processMessageService.sendMultiLevelApprovalComplete(caseId, "APPROVED");
            log.info("Approval completion message sent for dispute: {}", caseId);
        } catch (Exception e) {
            log.error("Failed to send approval completion message for dispute: {}", caseId, e);
        }
        
        log.info("Dispute {} has been successfully approved and finalized", caseId);
    }
} 