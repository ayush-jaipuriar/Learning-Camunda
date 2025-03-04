package com.example.disputeresolutionsystem.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class DisputeSubmissionRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Dispute type is required")
    private String disputeType;
    
    @NotBlank(message = "Credit report ID is required")
    private String creditReportId;
    
    @NotEmpty(message = "At least one document is required")
    private List<MultipartFile> documents;
} 