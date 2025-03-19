package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.model.AuditLog;
import com.example.disputeresolutionsystem.model.ComplianceReport;
import com.example.disputeresolutionsystem.service.AuditService;
import com.example.disputeresolutionsystem.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportingService reportingService;
    private final AuditService auditService;
    private final RuntimeService runtimeService;

    /**
     * Generate a monthly compliance report
     */
    @PostMapping("/monthly")
    public ResponseEntity<Map<String, Object>> generateMonthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "JSON") String format) {
        
        log.info("Generating monthly report for {}-{} in {} format", year, month, format);
        
        // Validate parameters
        if (year < 2000 || year > 3000 || month < 1 || month > 12) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid year or month"
            ));
        }
        
        ComplianceReport.ReportFormat reportFormat;
        try {
            reportFormat = ComplianceReport.ReportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid report format. Valid formats are: PDF, CSV, JSON, HTML"
            ));
        }
        
        // Generate the report
        YearMonth yearMonth = YearMonth.of(year, month);
        ComplianceReport report = reportingService.generateMonthlyReport(yearMonth, reportFormat);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Report generated successfully");
        response.put("reportId", report.getReportId());
        response.put("reportPeriod", yearMonth.toString());
        response.put("format", report.getFormat().toString());
        response.put("totalDisputes", report.getTotalDisputes());
        response.put("resolvedDisputes", report.getResolvedDisputes());
        response.put("slaViolations", report.getSlaViolations());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Start the Camunda report generation process
     */
    @PostMapping("/process/start")
    public ResponseEntity<Map<String, Object>> startReportProcess(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false, defaultValue = "JSON") String format,
            @RequestParam(required = false) String recipients) {
        
        // Prepare process variables
        Map<String, Object> variables = new HashMap<>();
        
        if (year != null) {
            variables.put("year", year);
        }
        if (month != null) {
            variables.put("month", month);
        }
        
        variables.put("reportFormat", format);
        variables.put("recipients", recipients);
        variables.put("startedBy", "api");
        
        // Start the process
        String processInstanceId = runtimeService.startProcessInstanceByKey(
                "compliance-report-generation", variables).getId();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Report generation process started");
        response.put("processInstanceId", processInstanceId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Distribute a report
     */
    @PostMapping("/{reportId}/distribute")
    public ResponseEntity<Map<String, Object>> distributeReport(
            @PathVariable String reportId,
            @RequestParam(required = false) String recipients) {
        
        ComplianceReport report = reportingService.getReportById(reportId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<String> recipientList = null;
        if (recipients != null && !recipients.isEmpty()) {
            recipientList = List.of(recipients.split(","));
        }
        
        boolean success = reportingService.distributeReport(reportId, recipientList);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", success ? "success" : "error");
        response.put("message", success ? "Report distributed successfully" : "Failed to distribute report");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get a report by ID
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ComplianceReport> getReport(@PathVariable String reportId) {
        ComplianceReport report = reportingService.getReportById(reportId);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Download a report file
     */
    @GetMapping("/{reportId}/download")
    public ResponseEntity<Resource> downloadReport(@PathVariable String reportId) throws IOException {
        ComplianceReport report = reportingService.getReportById(reportId);
        if (report == null || report.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        Path path = Paths.get(report.getFilePath());
        Resource resource = new UrlResource(path.toUri());
        
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        String contentType = switch (report.getFormat()) {
            case PDF -> MediaType.APPLICATION_PDF_VALUE;
            case CSV -> "text/csv";
            case JSON -> MediaType.APPLICATION_JSON_VALUE;
            case HTML -> MediaType.TEXT_HTML_VALUE;
        };
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                .body(resource);
    }
    
    /**
     * Get SLA violation statistics for a time period
     */
    @GetMapping("/violations/stats")
    public ResponseEntity<Map<String, Object>> getSlaViolationStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        long violationCount = auditService.getSLAViolationCount(startDate, endDate);
        
        List<AuditLog> violationLogs = auditService.getActionAuditLogs(
                AuditLog.ActionType.SLA_VIOLATION, startDate, endDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalViolations", violationCount);
        response.put("period", Map.of(
            "start", startDate.toString(),
            "end", endDate.toString()
        ));
        response.put("violations", violationLogs);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retry failed report distributions
     */
    @PostMapping("/retry-distributions")
    public ResponseEntity<Map<String, Object>> retryFailedDistributions() {
        int retriedCount = reportingService.retryFailedDistributions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Retried failed distributions");
        response.put("retriedCount", retriedCount);
        
        return ResponseEntity.ok(response);
    }
} 