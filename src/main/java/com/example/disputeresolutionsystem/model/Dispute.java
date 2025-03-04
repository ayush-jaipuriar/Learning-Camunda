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
    private String priorityLevel;
    
    @CreationTimestamp
    private LocalDateTime submissionTimestamp;
    
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = "Submitted";
        }
    }
} 