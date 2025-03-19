package com.example.disputeresolutionsystem.service.impl;

import com.example.disputeresolutionsystem.model.ComplianceReport;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.ComplianceReportRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.AuditService;
import com.example.disputeresolutionsystem.service.ReportingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private final ComplianceReportRepository reportRepository;
    private final DisputeRepository disputeRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${app.reports.output-dir:./reports}")
    private String reportOutputDirectory;
    
    @Value("${app.reports.distribution.default-recipients:admin@example.com}")
    private String defaultRecipients;

    @Override
    @Transactional
    public ComplianceReport generateMonthlyReport(YearMonth yearMonth, ComplianceReport.ReportFormat format) {
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();
        
        log.info("Generating monthly compliance report for {}-{}", year, month);
        
        // Check if report already exists
        List<ComplianceReport> existingReports = reportRepository.findByYearAndMonth(year, month);
        if (!existingReports.isEmpty()) {
            log.info("Report already exists for period {}-{}", year, month);
            return existingReports.get(0);
        }
        
        // Get start and end date of the month
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay();
        
        // Query all disputes in the period
        List<Dispute> disputesInMonth = disputeRepository.findAll().stream()
            .filter(d -> {
                // Include disputes submitted in this period
                boolean submittedInPeriod = d.getSubmissionTimestamp() != null && 
                    d.getSubmissionTimestamp().isAfter(startOfMonth) && 
                    d.getSubmissionTimestamp().isBefore(endOfMonth);
                
                // Include disputes resolved in this period
                boolean resolvedInPeriod = d.getResolutionTimestamp() != null && 
                    d.getResolutionTimestamp().isAfter(startOfMonth) && 
                    d.getResolutionTimestamp().isBefore(endOfMonth);
                
                return submittedInPeriod || resolvedInPeriod;
            })
            .collect(Collectors.toList());
        
        // Calculate metrics
        int totalDisputes = disputesInMonth.size();
        
        List<Dispute> resolvedDisputes = disputesInMonth.stream()
            .filter(d -> d.getResolutionTimestamp() != null && 
                   d.getResolutionTimestamp().isAfter(startOfMonth) && 
                   d.getResolutionTimestamp().isBefore(endOfMonth))
            .collect(Collectors.toList());
        
        int resolvedDisputesCount = resolvedDisputes.size();
        
        // Calculate average resolution time
        double avgResolutionTimeHours = 0;
        if (!resolvedDisputes.isEmpty()) {
            double totalHours = resolvedDisputes.stream()
                .mapToDouble(d -> Duration.between(d.getSubmissionTimestamp(), d.getResolutionTimestamp()).toHours())
                .sum();
            avgResolutionTimeHours = totalHours / resolvedDisputesCount;
        }
        
        // Get SLA violations count
        long slaViolations = auditService.getSLAViolationCount(startOfMonth, endOfMonth);
        
        // Create report
        ComplianceReport report = new ComplianceReport();
        report.setYear(year);
        report.setMonth(month);
        report.setTotalDisputes(totalDisputes);
        report.setResolvedDisputes(resolvedDisputesCount);
        report.setSlaViolations((int) slaViolations);
        report.setAverageResolutionTimeHours(avgResolutionTimeHours);
        report.setFormat(format);
        report.setDistributionList(defaultRecipients);
        report.setGeneratedBy("system");
        report.setStatus(ComplianceReport.ReportStatus.GENERATED);
        
        // Generate report content as JSON
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("period", yearMonth.toString());
        reportData.put("totalDisputes", totalDisputes);
        reportData.put("resolvedDisputes", resolvedDisputesCount);
        reportData.put("slaViolations", slaViolations);
        reportData.put("averageResolutionTimeHours", avgResolutionTimeHours);
        
        try {
            String jsonContent = objectMapper.writeValueAsString(reportData);
            report.setReportContent(jsonContent);
            
            // Generate file if needed
            String fileName = generateReportFile(report, reportData);
            report.setFilePath(fileName);
            
        } catch (Exception e) {
            log.error("Failed to generate report content: {}", e.getMessage(), e);
            report.setReportContent("Error generating report: " + e.getMessage());
        }
        
        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceReport getReportById(String reportId) {
        return reportRepository.findByReportId(reportId).orElse(null);
    }

    @Override
    @Transactional
    public boolean distributeReport(String reportId, List<String> recipientEmails) {
        ComplianceReport report = reportRepository.findByReportId(reportId).orElse(null);
        if (report == null) {
            log.error("Report not found for distribution: {}", reportId);
            return false;
        }
        
        // Update report status to simulate distribution
        report.setStatus(ComplianceReport.ReportStatus.DISTRIBUTED);
        report.setDistributionTimestamp(LocalDateTime.now());
        
        if (recipientEmails != null && !recipientEmails.isEmpty()) {
            report.setDistributionList(String.join(",", recipientEmails));
        }
        
        log.info("Distributing report {} to recipients: {}", reportId, report.getDistributionList());
        reportRepository.save(report);
        
        return true;
    }

    @Override
    @Transactional
    public int retryFailedDistributions() {
        List<ComplianceReport> failedReports = reportRepository.findFailedDistributionsAfterDate(
            LocalDateTime.now().minusDays(30));
        
        int successCount = 0;
        for (ComplianceReport report : failedReports) {
            List<String> recipients = List.of(report.getDistributionList().split(","));
            if (distributeReport(report.getReportId(), recipients)) {
                successCount++;
            }
        }
        
        return successCount;
    }

    @Override
    @Transactional
    public int generateYearlyReports(int year, ComplianceReport.ReportFormat format) {
        int currentYear = LocalDateTime.now().getYear();
        int currentMonth = LocalDateTime.now().getMonthValue();
        
        int reportsGenerated = 0;
        
        // Generate reports for all completed months
        for (int month = 1; month <= 12; month++) {
            // Skip future months
            if (year == currentYear && month >= currentMonth) {
                continue;
            }
            
            YearMonth yearMonth = YearMonth.of(year, month);
            ComplianceReport report = generateMonthlyReport(yearMonth, format);
            if (report != null && report.getReportId() != null) {
                reportsGenerated++;
            }
        }
        
        return reportsGenerated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceReport> getReportsForPeriod(YearMonth startYearMonth, YearMonth endYearMonth) {
        List<ComplianceReport> result = new ArrayList<>();
        
        YearMonth current = startYearMonth;
        while (!current.isAfter(endYearMonth)) {
            List<ComplianceReport> monthReports = reportRepository.findByYearAndMonth(
                current.getYear(), current.getMonthValue());
            result.addAll(monthReports);
            current = current.plusMonths(1);
        }
        
        return result;
    }
    
    @Scheduled(cron = "0 0 1 1 * ?") // Run at 1:00 AM on the first day of each month
    @Transactional
    public void generateMonthlyReportScheduled() {
        // Generate report for the previous month
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        ComplianceReport report = generateMonthlyReport(previousMonth, ComplianceReport.ReportFormat.JSON);
        
        // Auto-distribute if configuration allows
        if (report != null && report.getReportId() != null) {
            distributeReport(report.getReportId(), null);
        }
    }
    
    private String generateReportFile(ComplianceReport report, Map<String, Object> reportData) throws IOException {
        // Create directory if it doesn't exist
        Path dirPath = Paths.get(reportOutputDirectory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        YearMonth yearMonth = YearMonth.of(report.getYear(), report.getMonth());
        String fileName = String.format("%s/compliance_report_%s%s", 
                                       reportOutputDirectory,
                                       yearMonth,
                                       getFileExtensionForFormat(report.getFormat()));
        
        try (FileWriter writer = new FileWriter(fileName)) {
            // For simplicity, we're just writing JSON for all formats in this example
            // In a real implementation, you would use appropriate libraries for each format
            writer.write(objectMapper.writeValueAsString(reportData));
        }
        
        return fileName;
    }
    
    private String getFileExtensionForFormat(ComplianceReport.ReportFormat format) {
        return switch (format) {
            case PDF -> ".pdf";
            case CSV -> ".csv";
            case JSON -> ".json";
            case HTML -> ".html";
        };
    }
} 