package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Delegate that starts the multi-level approval process for high-complexity disputes
 */
@Slf4j
@Component("multiLevelApprovalStartDelegate")
@RequiredArgsConstructor
public class MultiLevelApprovalStartDelegate implements JavaDelegate {

    private final DisputeRepository disputeRepository;
    private final RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        log.info("Evaluating dispute for multi-level approval: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            execution.setVariable("multiLevelApprovalStarted", false);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        boolean requiresMultiLevelApproval = isHighComplexity(dispute);
        
        if (requiresMultiLevelApproval) {
            log.info("Starting multi-level approval process for dispute: {}", caseId);
            
            // Prepare variables for the new process
            Map<String, Object> variables = new HashMap<>();
            variables.put("caseId", caseId);
            variables.put("userId", dispute.getUserId());
            variables.put("disputeType", dispute.getDisputeType());
            variables.put("priorityLevel", dispute.getPriorityLevel().toString());
            variables.put("complexityLevel", dispute.getComplexityLevel().toString());
            
            // Start the multi-level approval process
            String processInstanceId = runtimeService
                    .startProcessInstanceByKey("multi_level_approval_process", caseId, variables)
                    .getProcessInstanceId();
            
            // Update the main process with the child process ID
            execution.setVariable("multiLevelApprovalProcessId", processInstanceId);
            execution.setVariable("multiLevelApprovalStarted", true);
            
            // Update the dispute status
            dispute.setStatus("In Multi-Level Approval");
            disputeRepository.save(dispute);
            
            log.info("Multi-level approval process started for dispute {}: Process ID {}", 
                    caseId, processInstanceId);
        } else {
            log.info("Dispute {} does not require multi-level approval", caseId);
            execution.setVariable("multiLevelApprovalStarted", false);
        }
    }
    
    /**
     * Check if the dispute is high complexity or high risk
     */
    private boolean isHighComplexity(Dispute dispute) {
        return dispute.getComplexityLevel() == Dispute.ComplexityLevel.HIGH_RISK || 
               dispute.getComplexityLevel() == Dispute.ComplexityLevel.COMPLEX ||
               dispute.getPriorityLevel() == Dispute.PriorityLevel.HIGH ||
               dispute.getPriorityLevel() == Dispute.PriorityLevel.CRITICAL;
    }
} 