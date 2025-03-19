package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributeReportDelegate implements JavaDelegate {

    private final ReportingService reportingService;
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing Distribute Report Delegate");
        
        // Extract process variables
        String reportId = (String) execution.getVariable("reportId");
        String recipientsList = (String) execution.getVariable("recipients");
        
        if (reportId == null || reportId.isEmpty()) {
            log.error("No report ID provided for distribution");
            execution.setVariable("distributionSuccess", false);
            execution.setVariable("errorMessage", "No report ID provided");
            return;
        }
        
        // Parse recipients list
        List<String> recipients = null;
        if (recipientsList != null && !recipientsList.isEmpty()) {
            recipients = Arrays.asList(recipientsList.split(","));
        }
        
        log.info("Distributing report {} to recipients", reportId);
        
        // Distribute the report
        boolean success = reportingService.distributeReport(reportId, recipients);
        
        // Set process variables
        execution.setVariable("distributionSuccess", success);
        
        if (success) {
            log.info("Report distribution completed successfully");
        } else {
            log.warn("Report distribution failed");
            execution.setVariable("errorMessage", "Failed to distribute report");
        }
    }
} 