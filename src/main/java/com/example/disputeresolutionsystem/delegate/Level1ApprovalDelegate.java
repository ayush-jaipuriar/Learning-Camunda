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
 * Delegate that assigns a Level 1 approver for a high-complexity dispute
 */
@Component
public class Level1ApprovalDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(Level1ApprovalDelegate.class);

    private final DisputeRepository disputeRepository;
    private final ApproverAssignmentService approverAssignmentService;

    @Autowired
    public Level1ApprovalDelegate(DisputeRepository disputeRepository, 
                                ApproverAssignmentService approverAssignmentService) {
        this.disputeRepository = disputeRepository;
        this.approverAssignmentService = approverAssignmentService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        logger.info("Assigning Level 1 approver for dispute ID: {}", caseId);

        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            
            // Assign a senior officer as the Level 1 approver
            boolean assignmentSuccess = approverAssignmentService.assignLevel1Approver(dispute);
            
            if (assignmentSuccess) {
                logger.info("Successfully assigned Level 1 approver: {} for dispute ID: {}", 
                    dispute.getLevel1ApproverUsername(), caseId);
                
                // Update process variables
                execution.setVariable("level1ApproverUsername", dispute.getLevel1ApproverUsername());
                execution.setVariable("level1AssignmentSuccessful", true);
            } else {
                logger.warn("Failed to assign Level 1 approver for dispute ID: {}", caseId);
                execution.setVariable("level1AssignmentSuccessful", false);
                execution.setVariable("failedAssignmentLevel", 1);
            }
            
            // Save the updated dispute
            disputeRepository.save(dispute);
        } else {
            logger.error("Unable to find dispute with ID: {}", caseId);
            execution.setVariable("level1AssignmentSuccessful", false);
            execution.setVariable("failedAssignmentLevel", 1);
        }
    }
} 