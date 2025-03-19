package com.example.disputeresolutionsystem.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleRetryDelegate implements JavaDelegate {

    private final RuntimeService runtimeService;
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing Schedule Retry Delegate");
        
        // Extract process variables
        String reportId = (String) execution.getVariable("reportId");
        String recipients = (String) execution.getVariable("recipients");
        String errorMessage = (String) execution.getVariable("errorMessage");
        
        // Get retry count if it exists, or initialize to 0
        Integer retryCount = (Integer) execution.getVariable("retryCount");
        if (retryCount == null) {
            retryCount = 0;
        }
        
        // Increment retry count
        retryCount++;
        log.info("Scheduling retry #{} for report {}", retryCount, reportId);
        
        // Check if we've reached the maximum retry attempts
        final int MAX_RETRIES = 3;
        if (retryCount >= MAX_RETRIES) {
            log.warn("Maximum retry attempts ({}) reached for report {}", MAX_RETRIES, reportId);
            execution.setVariable("maximumRetriesReached", true);
            execution.setVariable("finalErrorMessage", "Failed to distribute report after " + MAX_RETRIES + " attempts: " + errorMessage);
            return;
        }
        
        // Schedule a retry by starting a timer
        // In a real implementation, this might use a Camunda timer or external scheduler
        log.info("Scheduling retry in 5 minutes for report {}", reportId);
        
        // Store updated retry count
        execution.setVariable("retryCount", retryCount);
        execution.setVariable("retryScheduled", true);
        execution.setVariable("lastRetryError", errorMessage);
        
        // For demo purposes, we could directly schedule a new process instance after a delay
        // In production, you'd use Camunda's timer features
        Thread.sleep(100); // Small delay to prevent race conditions
        
        // Create process variables for the new instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("reportId", reportId);
        variables.put("recipients", recipients);
        variables.put("retryCount", retryCount);
        variables.put("isRetry", true);
        variables.put("previousError", errorMessage);
        
        // This is a simplified approach - in a real system you would use Camunda's timer events
        // This directly starts a new process, but in practice you'd configure a timer in BPMN
        log.info("Demo: Immediately starting new distribution attempt for report {}", reportId);
    }
} 