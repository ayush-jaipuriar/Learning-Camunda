package com.example.disputeresolutionsystem.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

/**
 * Service for handling process message correlation between processes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessMessageServiceImpl {

    private final RuntimeService runtimeService;
    
    /**
     * Send a message to notify that the multi-level approval process is complete
     * @param caseId The dispute case ID (used for correlation)
     * @param finalStatus The final status of the approval process
     */
    public void sendMultiLevelApprovalComplete(String caseId, String finalStatus) {
        log.info("Sending multi-level approval complete message for caseId: {} with status: {}", 
                caseId, finalStatus);
        
        try {
            runtimeService.createMessageCorrelation("MultiLevelApprovalComplete")
                    .processInstanceBusinessKey(caseId)
                    .setVariable("finalApprovalStatus", finalStatus)
                    .correlate();
            
            log.info("Successfully correlated MultiLevelApprovalComplete message for caseId: {}", caseId);
        } catch (Exception e) {
            log.error("Failed to correlate MultiLevelApprovalComplete message for caseId: {}", caseId, e);
        }
    }
} 