package com.example.disputeresolutionsystem.repository;

import com.example.disputeresolutionsystem.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, String> {
} 