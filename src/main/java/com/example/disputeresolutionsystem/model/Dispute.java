package com.example.disputeresolutionsystem.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
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
        // Initialize escalated field to false by default
        // This is just for new entities, existing ones will be handled by the SQL migration
    }
} 