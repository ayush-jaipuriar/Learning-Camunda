package com.example.disputeresolutionsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DisputeSubmissionResponse {
    private String caseId;
    private String status;
    private String message;
} 