package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.MultiLevelApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Delegate that checks if a dispute requires multi-level approval based on complexity
 */
@Slf4j
@Component("complexityCheckDelegate")
@RequiredArgsConstructor
public class ComplexityCheckDelegate implements JavaDelegate {

    private final DisputeRepository disputeRepository;
    private final MultiLevelApprovalService approvalService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String caseId = (String) execution.getVariable("caseId");
        log.info("Checking complexity level for dispute: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            execution.setVariable("requiresMultiLevelApproval", false);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        boolean requiresMultiLevelApproval = approvalService.requiresMultiLevelApproval(dispute);
        
        execution.setVariable("requiresMultiLevelApproval", requiresMultiLevelApproval);
        
        log.info("Dispute {} complexity check: requires multi-level approval = {}", 
                caseId, requiresMultiLevelApproval);
        
        // Log details about the decision
        log.info("Dispute {} details: complexity = {}, priority = {}", 
                caseId, dispute.getComplexityLevel(), dispute.getPriorityLevel());
    }
} 