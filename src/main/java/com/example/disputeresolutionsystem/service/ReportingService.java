package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.ComplianceReport;

import java.time.YearMonth;
import java.util.List;

public interface ReportingService {
    
    /**
     * Generate a monthly compliance report
     * 
     * @param yearMonth The year and month for the report period
     * @param format The format to generate the report in
     * @return The generated compliance report
     */
    ComplianceReport generateMonthlyReport(YearMonth yearMonth, ComplianceReport.ReportFormat format);
    
    /**
     * Get a compliance report by its ID
     * 
     * @param reportId The ID of the report
     * @return The compliance report, if found
     */
    ComplianceReport getReportById(String reportId);
    
    /**
     * Distribute a compliance report to the specified recipients
     * 
     * @param reportId The ID of the report to distribute
     * @param recipientEmails A list of email addresses to send the report to
     * @return true if the distribution was successful, false otherwise
     */
    boolean distributeReport(String reportId, List<String> recipientEmails);
    
    /**
     * Retry distribution for failed reports
     * 
     * @return The number of successfully retried distributions
     */
    int retryFailedDistributions();
    
    /**
     * Generate all reports for a specific year
     * 
     * @param year The year to generate reports for
     * @param format The format to generate the reports in
     * @return The number of reports generated
     */
    int generateYearlyReports(int year, ComplianceReport.ReportFormat format);
    
    /**
     * Get reports for a specific time period
     * 
     * @param startYearMonth The start of the period
     * @param endYearMonth The end of the period
     * @return A list of reports in the specified period
     */
    List<ComplianceReport> getReportsForPeriod(YearMonth startYearMonth, YearMonth endYearMonth);
} 