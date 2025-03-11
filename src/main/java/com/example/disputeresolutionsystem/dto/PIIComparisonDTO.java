package com.example.disputeresolutionsystem.dto;

import com.example.disputeresolutionsystem.model.Dispute.PIIValidationStatus;
import lombok.Data;

@Data
public class PIIComparisonDTO {
    // Submitted information from the dispute
    private String submittedUserFullName;
    private String submittedUserAddress;
    private String submittedUserPhoneNumber;
    private String submittedUserEmailAddress;
    
    // Database information from the UserPII table
    private String databaseUserFullName;
    private String databaseUserAddress;
    private String databaseUserPhoneNumber;
    private String databaseUserEmailAddress;
    
    // Match indicators
    private boolean fullNameMatch;
    private boolean addressMatch;
    private boolean phoneNumberMatch;
    private boolean emailMatch;
    
    // Overall validation status
    private PIIValidationStatus validationStatus;
    
    // Additional notes from reviewers
    private String validationNotes;
    
    // Helper method to calculate match percentages
    public int getMatchPercentage() {
        int matchCount = 0;
        int totalFields = 0;
        
        if (submittedUserFullName != null && databaseUserFullName != null) {
            totalFields++;
            if (fullNameMatch) matchCount++;
        }
        
        if (submittedUserAddress != null && databaseUserAddress != null) {
            totalFields++;
            if (addressMatch) matchCount++;
        }
        
        if (submittedUserPhoneNumber != null && databaseUserPhoneNumber != null) {
            totalFields++;
            if (phoneNumberMatch) matchCount++;
        }
        
        if (submittedUserEmailAddress != null && databaseUserEmailAddress != null) {
            totalFields++;
            if (emailMatch) matchCount++;
        }
        
        return totalFields > 0 ? (matchCount * 100 / totalFields) : 0;
    }
} 