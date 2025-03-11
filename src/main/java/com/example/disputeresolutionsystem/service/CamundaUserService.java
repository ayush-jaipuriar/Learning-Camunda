package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CamundaUserService {
    
    /**
     * Synchronize case officers with Camunda users
     * @param officers List of case officers to sync
     * @return Number of users synchronized
     */
    int syncOfficersWithCamundaUsers(List<CaseOfficer> officers);
    
    /**
     * Create a Camunda user for a case officer
     * @param officer Case officer to create user for
     * @return true if successful, false otherwise
     */
    boolean createCamundaUser(CaseOfficer officer);
    
    /**
     * Delete a Camunda user
     * @param username Username to delete
     * @return true if successful, false otherwise
     */
    boolean deleteCamundaUser(String username);
    
    /**
     * Clean up all Camunda identity resources (users, groups, memberships)
     * This is useful for testing and resetting the system
     */
    void cleanupAllIdentityResources();
    
    /**
     * Get the sanitized username for Camunda
     * @param username Original username
     * @return Sanitized username valid for Camunda
     */
    String getSanitizedUsername(String username);
} 