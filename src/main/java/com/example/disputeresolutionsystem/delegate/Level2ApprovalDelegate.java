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
 * Delegate that assigns a Level 2 approver (Senior Officer) for a high-complexity dispute
 */
@Component
public class Level2ApprovalDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(Level2ApprovalDelegate.class);

    private final DisputeRepository disputeRepository;
    private final ApproverAssignmentService approverAssignmentService;

    @Autowired
    public Level2ApprovalDelegate(DisputeRepository disputeRepository, 
                               ApproverAssignmentService approverAssignmentService) {
        this.disputeRepository = disputeRepository;
        this.approverAssignmentService = approverAssignmentService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        logger.info("Assigning Level 2 approver for dispute ID: {}", caseId);

        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            
            // Check if level 1 was approved before proceeding
            if (dispute.getLevel1ApprovalStatus() != Dispute.ApprovalStatus.APPROVED) {
                logger.warn("Cannot assign Level 2 approver for dispute ID: {} because Level 1 is not approved", caseId);
                execution.setVariable("level2AssignmentSuccessful", false);
                return;
            }
            
            // Assign a senior officer (different from Level 1) as the Level 2 approver
            boolean assignmentSuccess = approverAssignmentService.assignLevel2Approver(dispute);
            
            if (assignmentSuccess) {
                logger.info("Successfully assigned Level 2 approver: {} for dispute ID: {}", 
                    dispute.getLevel2ApproverUsername(), caseId);
                
                // Update process variables
                execution.setVariable("level2ApproverUsername", dispute.getLevel2ApproverUsername());
                execution.setVariable("level2AssignmentSuccessful", true);
            } else {
                logger.warn("Failed to assign Level 2 approver for dispute ID: {}", caseId);
                execution.setVariable("level2AssignmentSuccessful", false);
            }
            
            // Save the updated dispute
            disputeRepository.save(dispute);
        } else {
            logger.error("Unable to find dispute with ID: {}", caseId);
            execution.setVariable("level2AssignmentSuccessful", false);
        }
    }
} 