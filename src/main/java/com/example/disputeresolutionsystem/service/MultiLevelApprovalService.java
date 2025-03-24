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

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiLevelApprovalService {

    private final DisputeRepository disputeRepository;
    private final CaseOfficerRepository officerRepository;
    
    /**
     * Assign a Level 1 approver for a dispute
     *
     * @param caseId The dispute case ID
     * @return true if assignment was successful, false otherwise
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
        
        // Find an available Level 1 approver (regular case officer)
        List<CaseOfficer> availableOfficers = officerRepository.findByAvailableTrue();
        if (availableOfficers.isEmpty()) {
            log.warn("No available officers for Level 1 approval of dispute: {}", caseId);
            return false;
        }
        
        // Assign the first available officer
        CaseOfficer approver = availableOfficers.get(0);
        
        dispute.setLevel1ApproverUsername(approver.getUsername());
        dispute.setStatus("Level 1 Review");
        disputeRepository.save(dispute);
        
        log.info("Level 1 approver assigned for dispute {}: {}", caseId, approver.getUsername());
        return true;
    }
    
    /**
     * Assign a Level 2 approver (senior officer) for a dispute
     *
     * @param caseId The dispute case ID
     * @return true if assignment was successful, false otherwise
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
        
        // Find an available Level 2 approver (senior officer)
        List<CaseOfficer> seniorOfficers = officerRepository.findByRoleAndAvailableTrue("SENIOR_OFFICER");
        if (seniorOfficers.isEmpty()) {
            log.warn("No available senior officers for Level 2 approval of dispute: {}", caseId);
            return false;
        }
        
        // Assign the first available senior officer
        CaseOfficer approver = seniorOfficers.get(0);
        
        dispute.setLevel2ApproverUsername(approver.getUsername());
        dispute.setStatus("Level 2 Review");
        disputeRepository.save(dispute);
        
        log.info("Level 2 approver assigned for dispute {}: {}", caseId, approver.getUsername());
        return true;
    }
    
    /**
     * Assign a Level 3 approver (compliance team) for a dispute
     *
     * @param caseId The dispute case ID
     * @return true if assignment was successful, false otherwise
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
        
        // Find an available Level 3 approver (compliance officer)
        List<CaseOfficer> complianceOfficers = officerRepository.findByRoleAndAvailableTrue("COMPLIANCE_OFFICER");
        if (complianceOfficers.isEmpty()) {
            log.warn("No available compliance officers for Level 3 approval of dispute: {}", caseId);
            return false;
        }
        
        // Assign the first available compliance officer
        CaseOfficer approver = complianceOfficers.get(0);
        
        dispute.setLevel3ApproverUsername(approver.getUsername());
        dispute.setStatus("Level 3 Review");
        disputeRepository.save(dispute);
        
        log.info("Level 3 approver assigned for dispute {}: {}", caseId, approver.getUsername());
        return true;
    }
    
    /**
     * Record a Level 1 approval decision
     *
     * @param caseId    The dispute case ID
     * @param decision  The approval decision
     * @param notes     Notes explaining the decision
     * @param username  Username of the approver
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean recordLevel1Decision(String caseId, ApprovalStatus decision, String notes, String username) {
        log.info("Recording Level 1 decision for dispute {}: {}", caseId, decision);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            return false;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Verify the approver is authorized
        if (!username.equals(dispute.getLevel1ApproverUsername())) {
            log.warn("Unauthorized Level 1 approval attempt by {} for dispute {}", username, caseId);
            return false;
        }
        
        dispute.setLevel1ApprovalStatus(decision);
        dispute.setLevel1ApprovalNotes(notes);
        dispute.setLevel1ApprovalTimestamp(LocalDateTime.now());
        
        // Update the overall status based on the decision
        if (decision == ApprovalStatus.APPROVED) {
            dispute.setStatus("Level 1 Approved - Pending Level 2");
        } else if (decision == ApprovalStatus.REJECTED) {
            dispute.setStatus("Rejected at Level 1");
            dispute.setResolutionTimestamp(LocalDateTime.now());
        } else if (decision == ApprovalStatus.NEEDS_MORE_INFO) {
            dispute.setStatus("Level 1 Needs More Info");
        }
        
        disputeRepository.save(dispute);
        log.info("Level 1 decision recorded for dispute {}: {}", caseId, decision);
        return true;
    }
    
    /**
     * Record a Level 2 approval decision
     *
     * @param caseId    The dispute case ID
     * @param decision  The approval decision
     * @param notes     Notes explaining the decision
     * @param username  Username of the approver
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean recordLevel2Decision(String caseId, ApprovalStatus decision, String notes, String username) {
        log.info("Recording Level 2 decision for dispute {}: {}", caseId, decision);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            return false;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Verify the approver is authorized
        if (!username.equals(dispute.getLevel2ApproverUsername())) {
            log.warn("Unauthorized Level 2 approval attempt by {} for dispute {}", username, caseId);
            return false;
        }
        
        dispute.setLevel2ApprovalStatus(decision);
        dispute.setLevel2ApprovalNotes(notes);
        dispute.setLevel2ApprovalTimestamp(LocalDateTime.now());
        
        // Update the overall status based on the decision
        if (decision == ApprovalStatus.APPROVED) {
            dispute.setStatus("Level 2 Approved - Pending Level 3");
        } else if (decision == ApprovalStatus.REJECTED) {
            dispute.setStatus("Rejected at Level 2");
            dispute.setResolutionTimestamp(LocalDateTime.now());
        } else if (decision == ApprovalStatus.NEEDS_MORE_INFO) {
            dispute.setStatus("Level 2 Needs More Info");
        }
        
        disputeRepository.save(dispute);
        log.info("Level 2 decision recorded for dispute {}: {}", caseId, decision);
        return true;
    }
    
    /**
     * Record a Level 3 approval decision
     *
     * @param caseId    The dispute case ID
     * @param decision  The approval decision
     * @param notes     Notes explaining the decision
     * @param username  Username of the approver
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean recordLevel3Decision(String caseId, ApprovalStatus decision, String notes, String username) {
        log.info("Recording Level 3 decision for dispute {}: {}", caseId, decision);
        
        Optional<Dispute> disputeOpt = disputeRepository.findById(caseId);
        if (disputeOpt.isEmpty()) {
            log.warn("Dispute not found: {}", caseId);
            return false;
        }
        
        Dispute dispute = disputeOpt.get();
        
        // Verify the approver is authorized
        if (!username.equals(dispute.getLevel3ApproverUsername())) {
            log.warn("Unauthorized Level 3 approval attempt by {} for dispute {}", username, caseId);
            return false;
        }
        
        dispute.setLevel3ApprovalStatus(decision);
        dispute.setLevel3ApprovalNotes(notes);
        dispute.setLevel3ApprovalTimestamp(LocalDateTime.now());
        
        // Update the overall status based on the decision
        if (decision == ApprovalStatus.APPROVED) {
            dispute.setStatus("Final Approval - Dispute Resolved");
            dispute.setResolutionTimestamp(LocalDateTime.now());
        } else if (decision == ApprovalStatus.REJECTED) {
            dispute.setStatus("Rejected at Final Level");
            dispute.setResolutionTimestamp(LocalDateTime.now());
        } else if (decision == ApprovalStatus.NEEDS_MORE_INFO) {
            dispute.setStatus("Final Level Needs More Info");
        }
        
        disputeRepository.save(dispute);
        log.info("Level 3 decision recorded for dispute {}: {}", caseId, decision);
        return true;
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
        // Find another available officer, excluding the current one
        List<CaseOfficer> availableOfficers = officerRepository.findByUsernameNotAndAvailableTrue(
                dispute.getLevel1ApproverUsername());
        
        if (availableOfficers.isEmpty()) {
            log.warn("No alternative officers available for Level 1 escalation");
            return false;
        }
        
        // Assign to another officer
        CaseOfficer newApprover = availableOfficers.get(0);
        dispute.setLevel1ApproverUsername(newApprover.getUsername());
        disputeRepository.save(dispute);
        
        log.info("Escalated Level 1 review, reassigned to: {}", newApprover.getUsername());
        return true;
    }
    
    private boolean reassignLevel2Approver(Dispute dispute) {
        // Find another available senior officer, excluding the current one
        List<CaseOfficer> availableSeniorOfficers = officerRepository.findByRoleAndUsernameNotAndAvailableTrue(
                "SENIOR_OFFICER", dispute.getLevel2ApproverUsername());
        
        if (availableSeniorOfficers.isEmpty()) {
            log.warn("No alternative senior officers available for Level 2 escalation");
            return false;
        }
        
        // Assign to another senior officer
        CaseOfficer newApprover = availableSeniorOfficers.get(0);
        dispute.setLevel2ApproverUsername(newApprover.getUsername());
        disputeRepository.save(dispute);
        
        log.info("Escalated Level 2 review, reassigned to: {}", newApprover.getUsername());
        return true;
    }
    
    private boolean reassignLevel3Approver(Dispute dispute) {
        // Find another available compliance officer, excluding the current one
        List<CaseOfficer> availableComplianceOfficers = officerRepository.findByRoleAndUsernameNotAndAvailableTrue(
                "COMPLIANCE_OFFICER", dispute.getLevel3ApproverUsername());
        
        if (availableComplianceOfficers.isEmpty()) {
            log.warn("No alternative compliance officers available for Level 3 escalation");
            return false;
        }
        
        // Assign to another compliance officer
        CaseOfficer newApprover = availableComplianceOfficers.get(0);
        dispute.setLevel3ApproverUsername(newApprover.getUsername());
        disputeRepository.save(dispute);
        
        log.info("Escalated Level 3 review, reassigned to: {}", newApprover.getUsername());
        return true;
    }
    
    /**
     * Check if a dispute requires multi-level approval based on complexity and priority
     *
     * @param dispute The dispute to check
     * @return true if the dispute requires multi-level approval
     */
    public boolean requiresMultiLevelApproval(Dispute dispute) {
        // High-risk cases always require multi-level approval
        if (dispute.getComplexityLevel() == Dispute.ComplexityLevel.HIGH_RISK) {
            return true;
        }
        
        // Complex cases with high or critical priority require multi-level approval
        if (dispute.getComplexityLevel() == Dispute.ComplexityLevel.COMPLEX && 
            (dispute.getPriorityLevel() == Dispute.PriorityLevel.HIGH || 
             dispute.getPriorityLevel() == Dispute.PriorityLevel.CRITICAL)) {
            return true;
        }
        
        return false;
    }
} 