package com.example.disputeresolutionsystem.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class DisputeSubmissionRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Dispute type is required")
    private String disputeType;
    
    @NotBlank(message = "Credit report ID is required")
    private String creditReportId;
    
    private List<MultipartFile> documents;
} 