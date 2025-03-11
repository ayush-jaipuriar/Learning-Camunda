package com.example.disputeresolutionsystem.model;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "user_pii")
public class UserPII {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    
    private String fullName;
    private String address;
    private String phoneNumber;
    private String ssn;  // Social Security Number (or equivalent)
    private String dateOfBirth;
    private String emailAddress;
    
    // Many users can be associated with one case officer (for demo purposes)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id")
    private CaseOfficer caseOfficer;
} 