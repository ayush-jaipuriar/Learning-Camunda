package com.example.disputeresolutionsystem.model;

import lombok.Data;
import javax.persistence.*;

@Entity
@Data
@Table(name = "documents")
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Dispute dispute;
} 