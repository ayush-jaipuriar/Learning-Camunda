package com.example.disputeresolutionsystem.repository;

import com.example.disputeresolutionsystem.model.UserPII;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPIIRepository extends JpaRepository<UserPII, Long> {
    Optional<UserPII> findByUsername(String username);
    List<UserPII> findByCaseOfficerId(Long officerId);
    void deleteAllByCaseOfficerId(Long officerId);
} 