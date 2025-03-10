package com.example.disputeresolutionsystem.model;

import lombok.Data;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "case_officers")
public class CaseOfficer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String fullName;
    private String email;
    
    @Enumerated(EnumType.STRING)
    private OfficerLevel level;
    
    private int currentWorkload;
    private int maxWorkload;
    
    @OneToMany(mappedBy = "assignedOfficer")
    private List<Dispute> assignedDisputes = new ArrayList<>();
    
    public enum OfficerLevel {
        LEVEL_1,
        SENIOR,
        SUPERVISOR
    }
    
    public boolean canHandleMoreCases() {
        return currentWorkload < maxWorkload;
    }
} 