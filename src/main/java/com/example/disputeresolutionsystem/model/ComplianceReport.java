package com.example.disputeresolutionsystem.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
@Entity
@Table(name = "compliance_reports")
public class ComplianceReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String reportId;
    
    @Column(columnDefinition = "TEXT")
    private String reportContent;
    
    @Column(name = "report_period_year")
    private int year;
    
    @Column(name = "report_period_month")
    private int month;
    
    @Enumerated(EnumType.STRING)
    private ReportStatus status;
    
    private int totalDisputes;
    
    private int resolvedDisputes;
    
    private int slaViolations;
    
    private double averageResolutionTimeHours;
    
    private String distributionList;
    
    @Column(name = "report_format")
    @Enumerated(EnumType.STRING)
    private ReportFormat format;
    
    @CreationTimestamp
    private LocalDateTime generationTimestamp;
    
    private LocalDateTime distributionTimestamp;
    
    private String generatedBy;
    
    @Column(name = "file_path")
    private String filePath;
    
    public enum ReportStatus {
        GENERATED,
        DISTRIBUTED,
        DISTRIBUTION_FAILED
    }
    
    public enum ReportFormat {
        PDF,
        CSV,
        JSON,
        HTML
    }
    
    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = ReportStatus.GENERATED;
        }
        if (reportId == null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            reportId = "CR-" + yearMonth.toString() + "-" + System.currentTimeMillis() % 10000;
        }
    }
} 