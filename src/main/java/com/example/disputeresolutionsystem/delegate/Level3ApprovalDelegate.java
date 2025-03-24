package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.ApproverAssignmentService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Delegate that assigns a Level 3 approver (Compliance Team) for a high-complexity dispute
 */
@Component
public class Level3ApprovalDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(Level3ApprovalDelegate.class);

    private final DisputeRepository disputeRepository;
    private final ApproverAssignmentService approverAssignmentService;

    @Autowired
    public Level3ApprovalDelegate(DisputeRepository disputeRepository, 
                               ApproverAssignmentService approverAssignmentService) {
        this.disputeRepository = disputeRepository;
        this.approverAssignmentService = approverAssignmentService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        logger.info("Assigning Level 3 approver for dispute ID: {}", caseId);

        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            
            // Check if level 2 was approved before proceeding
            if (dispute.getLevel2ApprovalStatus() != Dispute.ApprovalStatus.APPROVED) {
                logger.warn("Cannot assign Level 3 approver for dispute ID: {} because Level 2 is not approved", caseId);
                execution.setVariable("level3AssignmentSuccessful", false);
                return;
            }
            
            // Assign a compliance officer as the Level 3 approver
            boolean assignmentSuccess = approverAssignmentService.assignLevel3Approver(dispute);
            
            if (assignmentSuccess) {
                logger.info("Successfully assigned Level 3 approver: {} for dispute ID: {}", 
                    dispute.getLevel3ApproverUsername(), caseId);
                
                // Update process variables
                execution.setVariable("level3ApproverUsername", dispute.getLevel3ApproverUsername());
                execution.setVariable("level3AssignmentSuccessful", true);
            } else {
                logger.warn("Failed to assign Level 3 approver for dispute ID: {}", caseId);
                execution.setVariable("level3AssignmentSuccessful", false);
            }
            
            // Save the updated dispute
            disputeRepository.save(dispute);
        } else {
            logger.error("Unable to find dispute with ID: {}", caseId);
            execution.setVariable("level3AssignmentSuccessful", false);
        }
    }
} 