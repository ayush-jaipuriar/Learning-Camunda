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
    private int caseLoad = 0;
    
    // Maximum number of cases an officer can handle
    private int maxCaseLoad = 10;
    
    @OneToMany(mappedBy = "assignedOfficer")
    private Set<Dispute> assignedDisputes = new HashSet<>();
    
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