package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.ComplianceReport;
import com.example.disputeresolutionsystem.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateReportDelegate implements JavaDelegate {

    private final ReportingService reportingService;
    
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Executing Generate Report Delegate");
        
        // Extract process variables
        int year = (int) execution.getVariable("year");
        int month = (int) execution.getVariable("month");
        String reportFormat = (String) execution.getVariable("reportFormat");
        
        // Default to current month if not specified
        if (year == 0 || month == 0) {
            YearMonth previousMonth = YearMonth.now().minusMonths(1);
            year = previousMonth.getYear();
            month = previousMonth.getMonthValue();
        }
        
        // Default format
        ComplianceReport.ReportFormat format = ComplianceReport.ReportFormat.JSON;
        if (reportFormat != null) {
            try {
                format = ComplianceReport.ReportFormat.valueOf(reportFormat.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid report format: {}. Using default: {}", reportFormat, format);
            }
        }
        
        log.info("Generating report for {}-{} in {} format", year, month, format);
        
        // Generate the report
        YearMonth yearMonth = YearMonth.of(year, month);
        ComplianceReport report = reportingService.generateMonthlyReport(yearMonth, format);
        
        // Set process variables
        execution.setVariable("reportId", report.getReportId());
        execution.setVariable("reportFilePath", report.getFilePath());
        
        // Determine if approval is needed (based on business rules)
        // For example, reports with high SLA violations might need approval
        boolean requiresApproval = report.getSlaViolations() >= 5;
        execution.setVariable("requiresApproval", requiresApproval);
        
        log.info("Report generation completed. Report ID: {}", report.getReportId());
    }
} 