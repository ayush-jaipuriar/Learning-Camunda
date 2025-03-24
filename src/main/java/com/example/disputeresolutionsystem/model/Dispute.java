package com.example.disputeresolutionsystem.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "disputes")
public class Dispute {
    
    @Id
    private String caseId;
    
    private String userId;
    private String disputeType;
    private String creditReportId;
    private String status;
    
    // User information from submitted document
    @Column(length = 100)
    private String submittedUserFullName;
    
    @Column(length = 255)
    private String submittedUserAddress;
    
    @Column(length = 20)
    private String submittedUserPhoneNumber;
    
    @Column(length = 100)
    private String submittedUserEmailAddress;
    
    // Additional details about the dispute
    @Column(length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    private PriorityLevel priorityLevel;
    
    @Enumerated(EnumType.STRING)
    private ComplexityLevel complexityLevel;
    
    @CreationTimestamp
    private LocalDateTime submissionTimestamp;
    
    private LocalDateTime assignmentTimestamp;
    private LocalDateTime escalationTimestamp;
    private LocalDateTime resolutionTimestamp;
    private LocalDateTime slaDeadline;
    
    private int remindersSent = 0;
    private boolean complianceReportGenerated = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id")
    private CaseOfficer assignedOfficer;
    
    // Initialize with default value
    private boolean escalated = false;
    
    // Store the result of PII validation
    @Enumerated(EnumType.STRING)
    private PIIValidationStatus piiValidationStatus = PIIValidationStatus.PENDING;
    
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();
    
    // Multi-level approval process fields
    @Enumerated(EnumType.STRING)
    private ApprovalStatus level1ApprovalStatus = ApprovalStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    private ApprovalStatus level2ApprovalStatus = ApprovalStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    private ApprovalStatus level3ApprovalStatus = ApprovalStatus.PENDING;
    
    @Column(length = 100)
    private String level1ApproverUsername;
    
    @Column(length = 100)
    private String level2ApproverUsername;
    
    @Column(length = 100)
    private String level3ApproverUsername;
    
    @Column(length = 1000)
    private String level1ApprovalNotes;
    
    @Column(length = 1000)
    private String level2ApprovalNotes;
    
    @Column(length = 1000)
    private String level3ApprovalNotes;
    
    private LocalDateTime level1ApprovalTimestamp;
    private LocalDateTime level2ApprovalTimestamp;
    private LocalDateTime level3ApprovalTimestamp;
    
    private boolean level1Escalated = false;
    private boolean level2Escalated = false;
    private boolean level3Escalated = false;
    
    public enum PriorityLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public enum ComplexityLevel {
        SIMPLE,
        COMPLEX,
        HIGH_RISK
    }
    
    public enum PIIValidationStatus {
        PENDING,      // Not yet reviewed
        MISMATCH,     // PII data doesn't match system records
        PARTIAL_MATCH, // Some fields match but not all
        MATCH,        // All PII data matches system records
        NOT_FOUND     // No matching user found in the system
    }
    
    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        NEEDS_MORE_INFO
    }
    
    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = "Submitted";
        }
        if (priorityLevel == null) {
            priorityLevel = PriorityLevel.MEDIUM;
        }
        if (complexityLevel == null) {
            complexityLevel = ComplexityLevel.SIMPLE;
        }
        if (piiValidationStatus == null) {
            piiValidationStatus = PIIValidationStatus.PENDING;
        }
        if (slaDeadline == null) {
            // Set default SLA deadline to 5 minutes after submission
            slaDeadline = LocalDateTime.now().plusMinutes(5);
        }
        // Initialize approval statuses
        if (level1ApprovalStatus == null) {
            level1ApprovalStatus = ApprovalStatus.PENDING;
        }
        if (level2ApprovalStatus == null) {
            level2ApprovalStatus = ApprovalStatus.PENDING;
        }
        if (level3ApprovalStatus == null) {
            level3ApprovalStatus = ApprovalStatus.PENDING;
        }
    }
} 