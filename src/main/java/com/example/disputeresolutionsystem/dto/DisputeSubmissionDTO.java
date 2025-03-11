package com.example.disputeresolutionsystem.dto;

import lombok.Data;

@Data
public class DisputeSubmissionDTO {
    private String userId;
    private String disputeType;
    private String creditReportId;
    
    // User information from the submitted document
    private String userFullName;
    private String userAddress;
    private String userPhoneNumber;
    private String userEmailAddress;
    
    // Additional details for the dispute
    private String description;
} 