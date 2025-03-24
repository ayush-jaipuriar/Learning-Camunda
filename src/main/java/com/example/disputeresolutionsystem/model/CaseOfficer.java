package com.example.disputeresolutionsystem.model;

import lombok.Data;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "case_officers")
public class CaseOfficer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    
    private String fullName;
    private String email;
    
    // New field for officer role
    @Column(length = 50)
    private String role = "OFFICER"; // Default role
    
    // Flag to indicate if the officer is available for new assignments
    private boolean available = true;
    
    // Number of currently assigned cases
    @Column(name = "case_load")
    private int caseLoad = 0;
    
    // Maximum number of cases an officer can handle
    @Column(name = "max_workload")
    private int maxCaseLoad = 10;
    
    @OneToMany(mappedBy = "assignedOfficer")
    private Set<Dispute> assignedDisputes = new HashSet<>();
    
    // Enum for officer levels
    public enum OfficerLevel {
        LEVEL_1,            // Regular case officer
        SENIOR,             // Senior officer for Level 1 and 2 approvals
        SUPERVISOR,         // Supervisor officer for oversight
        OFFICER,            // Legacy value - kept for compatibility
        SENIOR_OFFICER,     // Legacy value - kept for compatibility
        COMPLIANCE_OFFICER  // Compliance officer for Level 3 approvals
    }
    
    /**
     * Get the officer level (for compatibility with existing code)
     * @return The officer level based on the role
     */
    public OfficerLevel getLevel() {
        if (role == null) {
            return OfficerLevel.LEVEL_1;
        }
        
        switch (role) {
            case "SUPERVISOR":
                return OfficerLevel.SUPERVISOR;
            case "SENIOR_OFFICER":
                return OfficerLevel.SENIOR;
            case "COMPLIANCE_OFFICER":
                return OfficerLevel.SUPERVISOR; // Treating compliance as supervisors for legacy code
            default:
                return OfficerLevel.LEVEL_1;
        }
    }
    
    /**
     * Set the officer level (for compatibility with existing code)
     * @param level The officer level
     */
    public void setLevel(OfficerLevel level) {
        if (level == null) {
            this.role = "OFFICER";
            return;
        }
        
        switch (level) {
            case SUPERVISOR:
                this.role = "SUPERVISOR";
                break;
            case SENIOR:
                this.role = "SENIOR_OFFICER";
                break;
            case LEVEL_1:
            default:
                this.role = "OFFICER";
                break;
        }
    }
    
    /**
     * Get the current workload (for compatibility with existing code)
     * @return The current case load
     */
    public int getCurrentWorkload() {
        return this.caseLoad;
    }
    
    /**
     * Set the current workload (for compatibility with existing code)
     * @param workload The new workload value
     */
    public void setCurrentWorkload(int workload) {
        this.caseLoad = workload;
    }
    
    /**
     * Get the maximum workload (for compatibility with existing code)
     * @return The maximum case load
     */
    public int getMaxWorkload() {
        return this.maxCaseLoad;
    }
    
    /**
     * Set the maximum workload (for compatibility with existing code)
     * @param maxWorkload The new maximum workload value
     */
    public void setMaxWorkload(int maxWorkload) {
        this.maxCaseLoad = maxWorkload;
    }
    
    /**
     * Check if the officer can accept more cases
     * @return true if the officer can accept more cases
     */
    public boolean canAcceptMoreCases() {
        return available && caseLoad < maxCaseLoad;
    }
    
    /**
     * Increment the case load when a new case is assigned
     */
    public void incrementCaseLoad() {
        this.caseLoad++;
        if (this.caseLoad >= this.maxCaseLoad) {
            this.available = false;
        }
    }
    
    /**
     * Decrement the case load when a case is resolved
     */
    public void decrementCaseLoad() {
        if (this.caseLoad > 0) {
            this.caseLoad--;
        }
        if (this.caseLoad < this.maxCaseLoad) {
            this.available = true;
        }
    }
} 