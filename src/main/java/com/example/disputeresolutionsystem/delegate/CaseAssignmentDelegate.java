package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.CaseOfficer;
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
public class CaseAssignmentDelegate implements JavaDelegate {

    private final DisputeRepository disputeRepository;
    private final CaseAssignmentService caseAssignmentService;
    private final CamundaUserService camundaUserService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Get case ID from process variables
        String caseId = (String) execution.getVariable("caseId");
        log.info("Executing case assignment for dispute: {}", caseId);
        
        // Find the dispute
        Dispute dispute = disputeRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Dispute not found with ID: " + caseId));
        
        // Assign the dispute to an officer
        CaseOfficer assignedOfficer = caseAssignmentService.assignDisputeToOfficer(dispute);
        
        if (assignedOfficer != null) {
            // Ensure the officer is properly synced with Camunda
            camundaUserService.createCamundaUser(assignedOfficer);
            
            // Set process variables for the assigned officer
            execution.setVariable("assignedOfficerId", assignedOfficer.getId());
            execution.setVariable("assignedOfficerUsername", camundaUserService.getSanitizedUsername(assignedOfficer.getUsername()));
            execution.setVariable("assignedOfficerLevel", assignedOfficer.getLevel().toString());
            execution.setVariable("assignmentSuccessful", true);
            
            log.info("Dispute {} assigned to officer {} ({})", 
                    caseId, assignedOfficer.getUsername(), assignedOfficer.getLevel());
        } else {
            // No officer available for assignment
            execution.setVariable("assignmentSuccessful", false);
            log.warn("No officer available for assignment of dispute: {}", caseId);
        }
    }
} 