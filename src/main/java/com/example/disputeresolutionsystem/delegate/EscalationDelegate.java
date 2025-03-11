package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.CamundaUserService;
import com.example.disputeresolutionsystem.service.CaseAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationDelegate implements JavaDelegate {

    private final DisputeRepository disputeRepository;
    private final CaseAssignmentService caseAssignmentService;
    private final CamundaUserService camundaUserService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Get case ID from process variables
        String caseId = (String) execution.getVariable("caseId");
        log.info("Executing escalation for dispute: {}", caseId);
        
        // Find the dispute
        Dispute dispute = disputeRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Dispute not found with ID: " + caseId));
        
        // Escalate the dispute
        boolean escalationSuccessful = caseAssignmentService.escalateDispute(dispute);
        
        // Set process variables for the escalation result
        execution.setVariable("escalationSuccessful", escalationSuccessful);
        
        if (escalationSuccessful && dispute.getAssignedOfficer() != null) {
            // Ensure the officer is properly synced with Camunda
            camundaUserService.createCamundaUser(dispute.getAssignedOfficer());
            
            execution.setVariable("assignedOfficerId", dispute.getAssignedOfficer().getId());
            execution.setVariable("assignedOfficerUsername", 
                camundaUserService.getSanitizedUsername(dispute.getAssignedOfficer().getUsername()));
            execution.setVariable("assignedOfficerLevel", dispute.getAssignedOfficer().getLevel().toString());
            
            log.info("Dispute {} escalated and assigned to officer {} ({})", 
                    caseId, dispute.getAssignedOfficer().getUsername(), dispute.getAssignedOfficer().getLevel());
        } else {
            log.warn("Escalation completed for dispute {}, but no officer assigned", caseId);
        }
    }
} 