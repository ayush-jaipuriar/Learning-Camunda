package com.example.disputeresolutionsystem.repository;

import com.example.disputeresolutionsystem.model.ComplianceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceReportRepository extends JpaRepository<ComplianceReport, Long> {
    
    Optional<ComplianceReport> findByReportId(String reportId);
    
    List<ComplianceReport> findByYearAndMonth(int year, int month);
    
    List<ComplianceReport> findByStatus(ComplianceReport.ReportStatus status);
    
    @Query("SELECT c FROM ComplianceReport c WHERE c.year = ?1 ORDER BY c.month ASC")
    List<ComplianceReport> findAllReportsForYear(int year);
    
    @Query("SELECT c FROM ComplianceReport c WHERE c.status = 'DISTRIBUTION_FAILED' AND c.generationTimestamp > ?1")
    List<ComplianceReport> findFailedDistributionsAfterDate(LocalDateTime date);
    
    @Query("SELECT SUM(c.slaViolations) FROM ComplianceReport c WHERE c.year = ?1 AND c.month BETWEEN ?2 AND ?3")
    Integer getTotalSLAViolationsForPeriod(int year, int startMonth, int endMonth);
} 