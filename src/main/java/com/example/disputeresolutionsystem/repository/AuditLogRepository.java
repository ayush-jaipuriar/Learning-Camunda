package com.example.disputeresolutionsystem.repository;

import com.example.disputeresolutionsystem.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByCaseId(String caseId);
    
    List<AuditLog> findByActionType(AuditLog.ActionType actionType);
    
    List<AuditLog> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT a FROM AuditLog a WHERE a.caseId = ?1 ORDER BY a.timestamp DESC")
    List<AuditLog> findAuditHistoryForDispute(String caseId);
    
    @Query("SELECT a FROM AuditLog a WHERE a.actionType = 'SLA_VIOLATION' AND a.timestamp BETWEEN ?1 AND ?2")
    List<AuditLog> findSLAViolationsInPeriod(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType = 'SLA_VIOLATION' AND a.timestamp BETWEEN ?1 AND ?2")
    long countSLAViolationsInPeriod(LocalDateTime start, LocalDateTime end);
} 