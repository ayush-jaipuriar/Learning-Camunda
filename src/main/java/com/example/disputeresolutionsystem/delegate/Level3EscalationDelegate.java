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
public class Level3EscalationDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(Level3EscalationDelegate.class);

    private final DisputeRepository disputeRepository;
    private final NotificationService notificationService;

    @Autowired
    public Level3EscalationDelegate(DisputeRepository disputeRepository, 
                                  NotificationService notificationService) {
        this.disputeRepository = disputeRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        String level3Approver = (String) execution.getVariable("level3ApproverUsername");
        
        logger.info("Escalating Level 3 review for dispute ID: {}, approver: {}", caseId, level3Approver);

        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            
            // Mark the dispute as escalated at Level 3
            dispute.setLevel3Escalated(true);
            
            // Notify executive management about the escalation
            String escalationMessage = String.format(
                "CRITICAL ESCALATION ALERT: Level 3 approval for dispute %s has exceeded the time limit. " +
                "This dispute has now exceeded all approval time limits. " +
                "Assigned to: %s", caseId, level3Approver);
            
            notificationService.sendEscalationNotification(
                "executive_management", 
                "CRITICAL: Level 3 Approval Escalation", 
                escalationMessage
            );
            
            // Save changes to dispute
            disputeRepository.save(dispute);
            
            logger.info("Successfully escalated Level 3 review for dispute ID: {}", caseId);
            execution.setVariable("level3Escalated", true);
        } else {
            logger.error("Unable to find dispute with ID: {} for escalation", caseId);
        }
    }
} 