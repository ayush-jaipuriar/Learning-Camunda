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
        // Initialize escalated field to false by default
        // This is just for new entities, existing ones will be handled by the SQL migration
    }
} 