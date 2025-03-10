package com.example.disputeresolutionsystem.repository;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseOfficerRepository extends JpaRepository<CaseOfficer, Long> {
    
    List<CaseOfficer> findByLevel(CaseOfficer.OfficerLevel level);
    
    @Query("SELECT o FROM CaseOfficer o WHERE o.level = ?1 AND o.currentWorkload < o.maxWorkload ORDER BY o.currentWorkload ASC")
    List<CaseOfficer> findAvailableOfficersByLevel(CaseOfficer.OfficerLevel level);
    
    @Query("SELECT o FROM CaseOfficer o WHERE o.currentWorkload < o.maxWorkload ORDER BY o.currentWorkload ASC")
    List<CaseOfficer> findAllAvailableOfficers();
} 