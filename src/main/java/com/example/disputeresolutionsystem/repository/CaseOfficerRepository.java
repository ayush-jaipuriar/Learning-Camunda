package com.example.disputeresolutionsystem.repository;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseOfficerRepository extends JpaRepository<CaseOfficer, Long> {
    
    Optional<CaseOfficer> findByUsername(String username);
    
    List<CaseOfficer> findByRole(String role);
    
    @Query("SELECT c FROM CaseOfficer c WHERE c.role = :role AND c.available = :available AND c.caseLoad < c.maxCaseLoad")
    List<CaseOfficer> findByRoleAndAvailableAndCaseLoadLessThanMaxCaseLoad(
            @Param("role") String role, 
            @Param("available") boolean available);
    
    @Query("SELECT c FROM CaseOfficer c WHERE c.role = :role AND c.available = :available AND c.username != :username AND c.caseLoad < c.maxCaseLoad")
    List<CaseOfficer> findByRoleAndAvailableAndUsernameNotAndCaseLoadLessThanMaxCaseLoad(
            @Param("role") String role, 
            @Param("available") boolean available, 
            @Param("username") String excludeUsername);
} 