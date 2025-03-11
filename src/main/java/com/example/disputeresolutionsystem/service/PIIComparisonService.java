package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.dto.PIIComparisonDTO;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.Dispute.PIIValidationStatus;
import com.example.disputeresolutionsystem.model.UserPII;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PIIComparisonService {

    private final UserPIIService userPIIService;
    private final DisputeRepository disputeRepository;
    
    /**
     * Compare PII data from a dispute with data in the database
     */
    public PIIComparisonDTO comparePIIData(String caseId) {
        log.info("Comparing PII data for dispute: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found with ID: {}", caseId);
            return null;
        }
        
        Dispute dispute = disputeOpt.get();
        String userId = dispute.getUserId();
        
        // Create comparison DTO
        PIIComparisonDTO comparison = new PIIComparisonDTO();
        
        // Set submitted data
        comparison.setSubmittedUserFullName(dispute.getSubmittedUserFullName());
        comparison.setSubmittedUserAddress(dispute.getSubmittedUserAddress());
        comparison.setSubmittedUserPhoneNumber(dispute.getSubmittedUserPhoneNumber());
        comparison.setSubmittedUserEmailAddress(dispute.getSubmittedUserEmailAddress());
        
        // Try to find matching user in database
        Optional<UserPII> userPIIOpt = userPIIService.getPIIByUsername(userId);
        
        if (userPIIOpt.isEmpty()) {
            log.warn("No matching user found in database for userId: {}", userId);
            comparison.setValidationStatus(PIIValidationStatus.NOT_FOUND);
            return comparison;
        }
        
        UserPII userPII = userPIIOpt.get();
        
        // Set database data
        comparison.setDatabaseUserFullName(userPII.getFullName());
        comparison.setDatabaseUserAddress(userPII.getAddress());
        comparison.setDatabaseUserPhoneNumber(userPII.getPhoneNumber());
        comparison.setDatabaseUserEmailAddress(userPII.getEmailAddress());
        
        // Compare fields
        boolean fullNameMatch = compareStrings(dispute.getSubmittedUserFullName(), userPII.getFullName());
        boolean addressMatch = compareStrings(dispute.getSubmittedUserAddress(), userPII.getAddress());
        boolean phoneNumberMatch = compareStrings(dispute.getSubmittedUserPhoneNumber(), userPII.getPhoneNumber());
        boolean emailMatch = compareStrings(dispute.getSubmittedUserEmailAddress(), userPII.getEmailAddress());
        
        comparison.setFullNameMatch(fullNameMatch);
        comparison.setAddressMatch(addressMatch);
        comparison.setPhoneNumberMatch(phoneNumberMatch);
        comparison.setEmailMatch(emailMatch);
        
        // Determine overall validation status
        int matchCount = (fullNameMatch ? 1 : 0) + 
                         (addressMatch ? 1 : 0) + 
                         (phoneNumberMatch ? 1 : 0) + 
                         (emailMatch ? 1 : 0);
        
        if (matchCount == 4) {
            comparison.setValidationStatus(PIIValidationStatus.MATCH);
        } else if (matchCount >= 2) {
            comparison.setValidationStatus(PIIValidationStatus.PARTIAL_MATCH);
        } else {
            comparison.setValidationStatus(PIIValidationStatus.MISMATCH);
        }
        
        return comparison;
    }
    
    /**
     * Update the PII validation status of a dispute
     */
    @Transactional
    public void updatePIIValidationStatus(String caseId, PIIValidationStatus status, String notes) {
        log.info("Updating PII validation status for dispute {}: {}", caseId, status);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found with ID: {}", caseId);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        dispute.setPiiValidationStatus(status);
        disputeRepository.save(dispute);
        
        log.info("Updated PII validation status for dispute {}", caseId);
    }
    
    /**
     * Helper method to compare strings, handling null values
     */
    private boolean compareStrings(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        
        // Normalize strings for comparison (trim whitespace, ignore case)
        String normalized1 = str1.trim().toLowerCase();
        String normalized2 = str2.trim().toLowerCase();
        
        return normalized1.equals(normalized2);
    }
} 