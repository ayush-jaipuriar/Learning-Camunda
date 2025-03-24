package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.Dispute.ApprovalStatus;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiLevelApprovalService {

    private final DisputeRepository disputeRepository;
    private final CaseOfficerRepository officerRepository;
    private final NotificationService notificationService;
    
    /**
     * Check if a dispute requires multi-level approval
     * @param dispute The dispute to check
     * @return true if the dispute requires multi-level approval
     */
    public boolean requiresMultiLevelApproval(Dispute dispute) {
        return dispute.getComplexityLevel() == Dispute.ComplexityLevel.HIGH_RISK || 
               dispute.getComplexityLevel() == Dispute.ComplexityLevel.COMPLEX;
    }
    
    /**
     * Assign a Level 1 approver for a dispute
     * @param caseId The dispute case ID
     * @return true if assignment was successful
     */
    @Transactional
    public boolean assignLevel1Approver(String caseId) {
        log.info("Assigning Level 1 approver for dispute: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            return false;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Find an available senior officer
        List<CaseOfficer> availableOfficers = officerRepository.findByRoleAndAvailableTrue("SENIOR_OFFICER");
        
        if (availableOfficers.isEmpty()) {
            log.warn("No available officers for Level 1 approval of dispute: {}", caseId);
            return false;
        }
        
        // Take the first available officer (in a real system, we'd have a load balancing algorithm)
        CaseOfficer approver = availableOfficers.get(0);
        
        // Assign the approver
        dispute.setLevel1ApproverUsername(approver.getUsername());
        dispute.setStatus("Level 1 Review");
        disputeRepository.save(dispute);
        
        log.info("Level 1 approver assigned for dispute {}: {}", caseId, approver.getUsername());
        return true;
    }
    
    /**
     * Assign a Level 2 approver for a dispute
     * @param caseId The dispute case ID
     * @return true if assignment was successful
     */
    @Transactional
    public boolean assignLevel2Approver(String caseId) {
        log.info("Assigning Level 2 approver for dispute: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            return false;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Find an available senior officer (different from Level 1)
        List<CaseOfficer> seniorOfficers = officerRepository.findByRoleAndAvailableTrue("SENIOR_OFFICER");
        
        if (seniorOfficers.isEmpty()) {
            log.warn("No available senior officers for Level 2 approval of dispute: {}", caseId);
            return false;
        }
        
        // Take an officer that's not the same as Level 1
        CaseOfficer approver = seniorOfficers.stream()
            .filter(o -> !o.getUsername().equals(dispute.getLevel1ApproverUsername()))
            .findFirst()
            .orElse(seniorOfficers.get(0)); // If no other officer is available, use the same one (not ideal)
        
        // Assign the approver
        dispute.setLevel2ApproverUsername(approver.getUsername());
        dispute.setStatus("Level 2 Review");
        disputeRepository.save(dispute);
        
        log.info("Level 2 approver assigned for dispute {}: {}", caseId, approver.getUsername());
        return true;
    }
    
    /**
     * Assign a Level 3 approver for a dispute
     * @param caseId The dispute case ID
     * @return true if assignment was successful
     */
    @Transactional
    public boolean assignLevel3Approver(String caseId) {
        log.info("Assigning Level 3 approver for dispute: {}", caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            return false;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Find an available compliance officer
        List<CaseOfficer> complianceOfficers = officerRepository.findByRoleAndAvailableTrue("COMPLIANCE_OFFICER");
        
        if (complianceOfficers.isEmpty()) {
            log.warn("No available compliance officers for Level 3 approval of dispute: {}", caseId);
            return false;
        }
        
        // Take the first available compliance officer
        CaseOfficer approver = complianceOfficers.get(0);
        
        // Assign the approver
        dispute.setLevel3ApproverUsername(approver.getUsername());
        dispute.setStatus("Level 3 Review");
        disputeRepository.save(dispute);
        
        log.info("Level 3 approver assigned for dispute {}: {}", caseId, approver.getUsername());
        return true;
    }
    
    /**
     * Record the Level 1 approval decision
     * @param caseId The dispute case ID
     * @param decision The approval decision
     * @param username The username of the approver making the decision
     * @param notes Any notes provided with the decision
     * @return The updated dispute
     */
    @Transactional
    public Dispute recordLevel1Decision(String caseId, Dispute.ApprovalStatus decision, String username, String notes) {
        log.info("Recording Level 1 decision for dispute {}: {}", caseId, decision);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            throw new IllegalArgumentException("Dispute not found: " + caseId);
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Verify the user is the assigned approver
        if (!username.equals(dispute.getLevel1ApproverUsername())) {
            log.warn("Unauthorized Level 1 approval attempt by {} for dispute {}", username, caseId);
            throw new IllegalArgumentException("You are not authorized to approve this dispute at Level 1");
        }
        
        // Update the approval status
        dispute.setLevel1ApprovalStatus(decision);
        dispute.setLevel1ApprovalNotes(notes);
        dispute.setLevel1ApprovalTimestamp(LocalDateTime.now());
        
        // Update the dispute status based on the decision
        if (decision == Dispute.ApprovalStatus.APPROVED) {
            dispute.setStatus("Level 1 Approved - Pending Level 2");
        } else if (decision == Dispute.ApprovalStatus.REJECTED) {
            dispute.setStatus("Rejected at Level 1");
            dispute.setResolutionTimestamp(LocalDateTime.now());
        } else {
            dispute.setStatus("Level 1 Needs More Info");
        }
        
        // Save the updated dispute
        disputeRepository.save(dispute);
        log.info("Level 1 decision recorded for dispute {}: {}", caseId, decision);
        
        return dispute;
    }
    
    /**
     * Record the Level 2 approval decision
     * @param caseId The dispute case ID
     * @param decision The approval decision
     * @param username The username of the approver making the decision
     * @param notes Any notes provided with the decision
     * @return The updated dispute
     */
    @Transactional
    public Dispute recordLevel2Decision(String caseId, Dispute.ApprovalStatus decision, String username, String notes) {
        log.info("Recording Level 2 decision for dispute {}: {}", caseId, decision);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            throw new IllegalArgumentException("Dispute not found: " + caseId);
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Verify the user is the assigned approver
        if (!username.equals(dispute.getLevel2ApproverUsername())) {
            log.warn("Unauthorized Level 2 approval attempt by {} for dispute {}", username, caseId);
            throw new IllegalArgumentException("You are not authorized to approve this dispute at Level 2");
        }
        
        // Update the approval status
        dispute.setLevel2ApprovalStatus(decision);
        dispute.setLevel2ApprovalNotes(notes);
        dispute.setLevel2ApprovalTimestamp(LocalDateTime.now());
        
        // Update the dispute status based on the decision
        if (decision == Dispute.ApprovalStatus.APPROVED) {
            dispute.setStatus("Level 2 Approved - Pending Level 3");
        } else if (decision == Dispute.ApprovalStatus.REJECTED) {
            dispute.setStatus("Rejected at Level 2");
            dispute.setResolutionTimestamp(LocalDateTime.now());
        } else {
            dispute.setStatus("Level 2 Needs More Info");
        }
        
        // Save the updated dispute
        disputeRepository.save(dispute);
        log.info("Level 2 decision recorded for dispute {}: {}", caseId, decision);
        
        return dispute;
    }
    
    /**
     * Record the Level 3 approval decision
     * @param caseId The dispute case ID
     * @param decision The approval decision
     * @param username The username of the approver making the decision
     * @param notes Any notes provided with the decision
     * @return The updated dispute
     */
    @Transactional
    public Dispute recordLevel3Decision(String caseId, Dispute.ApprovalStatus decision, String username, String notes) {
        log.info("Recording Level 3 decision for dispute {}: {}", caseId, decision);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            throw new IllegalArgumentException("Dispute not found: " + caseId);
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Verify the user is the assigned approver
        if (!username.equals(dispute.getLevel3ApproverUsername())) {
            log.warn("Unauthorized Level 3 approval attempt by {} for dispute {}", username, caseId);
            throw new IllegalArgumentException("You are not authorized to approve this dispute at Level 3");
        }
        
        // Update the approval status
        dispute.setLevel3ApprovalStatus(decision);
        dispute.setLevel3ApprovalNotes(notes);
        dispute.setLevel3ApprovalTimestamp(LocalDateTime.now());
        
        // Update the dispute status based on the decision
        if (decision == Dispute.ApprovalStatus.APPROVED) {
            dispute.setStatus("Approved - All Levels");
            dispute.setResolutionTimestamp(LocalDateTime.now());
        } else if (decision == Dispute.ApprovalStatus.REJECTED) {
            dispute.setStatus("Rejected at Level 3");
            dispute.setResolutionTimestamp(LocalDateTime.now());
        } else {
            dispute.setStatus("Level 3 Needs More Info");
        }
        
        // Save the updated dispute
        disputeRepository.save(dispute);
        log.info("Level 3 decision recorded for dispute {}: {}", caseId, decision);
        
        return dispute;
    }
    
    /**
     * Handle escalation for a specific approval level
     *
     * @param caseId The dispute case ID
     * @param level  The approval level to escalate (1, 2, or 3)
     * @return true if escalation was successful
     */
    @Transactional
    public boolean escalateApproval(String caseId, int level) {
        log.info("Escalating Level {} approval for dispute: {}", level, caseId);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            return false;
        }
        
        Dispute dispute = disputeOpt.get();
        
        switch (level) {
            case 1:
                dispute.setLevel1Escalated(true);
                dispute.setStatus("Level 1 Review Escalated");
                // Try to find a new approver
                return reassignLevel1Approver(dispute);
                
            case 2:
                dispute.setLevel2Escalated(true);
                dispute.setStatus("Level 2 Review Escalated");
                // Try to find a new approver
                return reassignLevel2Approver(dispute);
                
            case 3:
                dispute.setLevel3Escalated(true);
                dispute.setStatus("Level 3 Review Escalated");
                // Try to find a new approver
                return reassignLevel3Approver(dispute);
                
            default:
                log.error("Invalid escalation level: {}", level);
                return false;
        }
    }
    
    private boolean reassignLevel1Approver(Dispute dispute) {
        log.info("Reassigning Level 1 approver for dispute: {}", dispute.getCaseId());
        
        // Get all available officers except the current one
        List<CaseOfficer> availableOfficers = officerRepository.findByRoleAndAvailableTrue("SENIOR_OFFICER")
            .stream()
            .filter(o -> !o.getUsername().equals(dispute.getLevel1ApproverUsername()))
            .collect(Collectors.toList());
        
        if (availableOfficers.isEmpty()) {
            log.warn("No available officers for Level 1 reassignment of dispute: {}", dispute.getCaseId());
            return false;
        }
        
        // Pick a random officer
        CaseOfficer approver = availableOfficers.get(new Random().nextInt(availableOfficers.size()));
        dispute.setLevel1ApproverUsername(approver.getUsername());
        
        disputeRepository.save(dispute);
        return true;
    }
    
    private boolean reassignLevel2Approver(Dispute dispute) {
        log.info("Reassigning Level 2 approver for dispute: {}", dispute.getCaseId());
        
        // Get all available senior officers except the current one and level 1 approver
        List<CaseOfficer> availableSeniorOfficers = officerRepository.findByRoleAndAvailableTrue("SENIOR_OFFICER")
            .stream()
            .filter(o -> !o.getUsername().equals(dispute.getLevel2ApproverUsername()) && 
                         !o.getUsername().equals(dispute.getLevel1ApproverUsername()))
            .collect(Collectors.toList());
        
        if (availableSeniorOfficers.isEmpty()) {
            log.warn("No available senior officers for Level 2 reassignment of dispute: {}", dispute.getCaseId());
            return false;
        }
        
        // Pick a random officer
        CaseOfficer approver = availableSeniorOfficers.get(new Random().nextInt(availableSeniorOfficers.size()));
        dispute.setLevel2ApproverUsername(approver.getUsername());
        
        disputeRepository.save(dispute);
        return true;
    }
    
    private boolean reassignLevel3Approver(Dispute dispute) {
        log.info("Reassigning Level 3 approver for dispute: {}", dispute.getCaseId());
        
        // Get all available compliance officers except the current one
        List<CaseOfficer> availableComplianceOfficers = officerRepository.findByRoleAndAvailableTrue("COMPLIANCE_OFFICER")
            .stream()
            .filter(o -> !o.getUsername().equals(dispute.getLevel3ApproverUsername()))
            .collect(Collectors.toList());
        
        if (availableComplianceOfficers.isEmpty()) {
            log.warn("No available compliance officers for Level 3 reassignment of dispute: {}", dispute.getCaseId());
            return false;
        }
        
        // Pick a random officer
        CaseOfficer approver = availableComplianceOfficers.get(new Random().nextInt(availableComplianceOfficers.size()));
        dispute.setLevel3ApproverUsername(approver.getUsername());
        
        disputeRepository.save(dispute);
        return true;
    }
} 