package com.example.disputeresolutionsystem.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private ActionType actionType;
    
    private String userId;
    
    private String caseId;
    
    private String previousStatus;
    
    private String newStatus;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "action_data", columnDefinition = "TEXT")
    private String actionData;
    
    @CreationTimestamp
    private LocalDateTime timestamp;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id")
    private Dispute dispute;
    
    public enum ActionType {
        SUBMISSION,
        ASSIGNMENT,
        REASSIGNMENT,
        ESCALATION,
        DECISION,
        STATUS_CHANGE,
        PII_VALIDATION,
        DOCUMENT_UPLOAD,
        REMINDER_SENT,
        SLA_VIOLATION,
        COMPLIANCE_REPORT_GENERATED
    }
} 