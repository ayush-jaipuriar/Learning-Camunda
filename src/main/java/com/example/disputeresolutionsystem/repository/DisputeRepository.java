package com.example.disputeresolutionsystem.repository;

import com.example.disputeresolutionsystem.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, String> {
    
    /**
     * Find disputes by status
     */
    List<Dispute> findByStatus(String status);
    
    /**
     * Find disputes by status and submission timestamp before a given time
     */
    List<Dispute> findByStatusAndSubmissionTimestampBefore(String status, LocalDateTime timestamp);
    
    /**
     * Find disputes by assigned officer
     */
    List<Dispute> findByAssignedOfficerId(Long officerId);
} 