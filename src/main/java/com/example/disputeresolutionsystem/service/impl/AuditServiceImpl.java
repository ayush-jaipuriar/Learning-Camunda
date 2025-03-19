package com.example.disputeresolutionsystem.service.impl;

import com.example.disputeresolutionsystem.model.AuditLog;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.AuditLogRepository;
import com.example.disputeresolutionsystem.service.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AuditLog logAction(AuditLog.ActionType actionType, Dispute dispute, String userId, 
                              String description, String previousStatus, String newStatus, 
                              Map<String, Object> actionData) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(actionType);
        auditLog.setDispute(dispute);
        auditLog.setCaseId(dispute.getCaseId());
        auditLog.setUserId(userId);
        auditLog.setDescription(description);
        auditLog.setPreviousStatus(previousStatus);
        auditLog.setNewStatus(newStatus);
        
        if (actionData != null) {
            try {
                auditLog.setActionData(objectMapper.writeValueAsString(actionData));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize action data: {}", e.getMessage());
                auditLog.setActionData("Error serializing data: " + e.getMessage());
            }
        }
        
        log.debug("Creating audit log: {}", auditLog);
        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getDisputeAuditHistory(String caseId) {
        return auditLogRepository.findAuditHistoryForDispute(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getActionAuditLogs(AuditLog.ActionType actionType, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByActionType(actionType).stream()
            .filter(log -> log.getTimestamp().isAfter(start) && log.getTimestamp().isBefore(end))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getUserAuditLogs(String userId, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUserIdAndTimestampBetween(userId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public long getSLAViolationCount(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.countSLAViolationsInPeriod(start, end);
    }
} 