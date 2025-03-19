package com.example.disputeresolutionsystem.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogDistributionDelegate implements JavaDelegate {
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing Log Distribution Delegate");
        
        // Extract process variables
        String reportId = (String) execution.getVariable("reportId");
        String recipients = (String) execution.getVariable("recipients");
        
        // Log successful distribution
        log.info("Report {} was successfully distributed to: {}", reportId, recipients);
        
        // In a more comprehensive system, this might write to an external logging system
        // or perform additional notification actions
        
        // Set successful completion variables if needed
        execution.setVariable("logTimestamp", System.currentTimeMillis());
        execution.setVariable("distributionCompleted", true);
    }
} 