package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.NotificationService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class Level1EscalationDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(Level1EscalationDelegate.class);

    private final DisputeRepository disputeRepository;
    private final NotificationService notificationService;

    @Autowired
    public Level1EscalationDelegate(DisputeRepository disputeRepository, 
                                  NotificationService notificationService) {
        this.disputeRepository = disputeRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        String level1Approver = (String) execution.getVariable("level1ApproverUsername");
        
        logger.info("Escalating Level 1 review for dispute ID: {}, approver: {}", caseId, level1Approver);

        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            
            // Mark the dispute as escalated at Level 1
            dispute.setLevel1Escalated(true);
            
            // Notify management about the escalation
            String escalationMessage = String.format(
                "ESCALATION ALERT: Level 1 approval for dispute %s has exceeded the time limit. Assigned to: %s",
                caseId, level1Approver);
            
            notificationService.sendEscalationNotification(
                "supervisor_team", 
                "Level 1 Approval Escalation", 
                escalationMessage
            );
            
            // Save changes to dispute
            disputeRepository.save(dispute);
            
            logger.info("Successfully escalated Level 1 review for dispute ID: {}", caseId);
            execution.setVariable("level1Escalated", true);
        } else {
            logger.error("Unable to find dispute with ID: {} for escalation", caseId);
        }
    }
} 