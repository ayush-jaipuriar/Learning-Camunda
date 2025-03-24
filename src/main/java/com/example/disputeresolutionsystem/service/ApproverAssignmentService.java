package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ApproverAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(ApproverAssignmentService.class);

    private final CaseOfficerRepository caseOfficerRepository;
    private final DisputeRepository disputeRepository;

    @Autowired
    public ApproverAssignmentService(CaseOfficerRepository caseOfficerRepository,
                                   DisputeRepository disputeRepository) {
        this.caseOfficerRepository = caseOfficerRepository;
        this.disputeRepository = disputeRepository;
    }

    @Transactional
    public boolean assignLevel1Approver(Dispute dispute) {
        logger.info("Attempting to assign Level 1 approver for dispute: {}", dispute.getCaseId());
        
        // Find available senior officers
        List<CaseOfficer> availableSeniorOfficers = caseOfficerRepository
            .findByRoleAndAvailableAndCaseLoadLessThanMaxCaseLoad("SENIOR_OFFICER", true);
        
        if (availableSeniorOfficers.isEmpty()) {
            logger.warn("No available senior officers found for Level 1 approval");
            return false;
        }
        
        // Select officer with lowest current caseload
        CaseOfficer selectedOfficer = availableSeniorOfficers.stream()
            .sorted((o1, o2) -> Integer.compare(o1.getCaseLoad(), o2.getCaseLoad()))
            .findFirst()
            .orElse(null);
        
        if (selectedOfficer == null) {
            return false;
        }
        
        // Assign the officer
        dispute.setLevel1ApproverUsername(selectedOfficer.getUsername());
        dispute.setLevel1ApprovalStatus(Dispute.ApprovalStatus.PENDING);
        
        // Update the officer's caseload
        selectedOfficer.setCaseLoad(selectedOfficer.getCaseLoad() + 1);
        caseOfficerRepository.save(selectedOfficer);
        
        logger.info("Successfully assigned Level 1 approver: {} to dispute: {}", 
            selectedOfficer.getUsername(), dispute.getCaseId());
        
        return true;
    }

    @Transactional
    public boolean assignLevel2Approver(Dispute dispute) {
        logger.info("Attempting to assign Level 2 approver for dispute: {}", dispute.getCaseId());
        
        // Find available senior officers who are not the same as Level 1 approver
        List<CaseOfficer> availableSeniorOfficers = caseOfficerRepository
            .findByRoleAndAvailableAndUsernameNotAndCaseLoadLessThanMaxCaseLoad(
                "SENIOR_OFFICER", true, dispute.getLevel1ApproverUsername());
        
        if (availableSeniorOfficers.isEmpty()) {
            logger.warn("No available senior officers found for Level 2 approval");
            return false;
        }
        
        // Select officer with lowest current caseload
        CaseOfficer selectedOfficer = availableSeniorOfficers.stream()
            .sorted((o1, o2) -> Integer.compare(o1.getCaseLoad(), o2.getCaseLoad()))
            .findFirst()
            .orElse(null);
        
        if (selectedOfficer == null) {
            return false;
        }
        
        // Assign the officer
        dispute.setLevel2ApproverUsername(selectedOfficer.getUsername());
        dispute.setLevel2ApprovalStatus(Dispute.ApprovalStatus.PENDING);
        
        // Update the officer's caseload
        selectedOfficer.setCaseLoad(selectedOfficer.getCaseLoad() + 1);
        caseOfficerRepository.save(selectedOfficer);
        
        logger.info("Successfully assigned Level 2 approver: {} to dispute: {}", 
            selectedOfficer.getUsername(), dispute.getCaseId());
        
        return true;
    }

    @Transactional
    public boolean assignLevel3Approver(Dispute dispute) {
        logger.info("Attempting to assign Level 3 approver for dispute: {}", dispute.getCaseId());
        
        // Find available compliance officers
        List<CaseOfficer> availableComplianceOfficers = caseOfficerRepository
            .findByRoleAndAvailableAndCaseLoadLessThanMaxCaseLoad("COMPLIANCE_OFFICER", true);
        
        if (availableComplianceOfficers.isEmpty()) {
            logger.warn("No available compliance officers found for Level 3 approval");
            return false;
        }
        
        // Select officer with lowest current caseload
        CaseOfficer selectedOfficer = availableComplianceOfficers.stream()
            .sorted((o1, o2) -> Integer.compare(o1.getCaseLoad(), o2.getCaseLoad()))
            .findFirst()
            .orElse(null);
        
        if (selectedOfficer == null) {
            return false;
        }
        
        // Assign the officer
        dispute.setLevel3ApproverUsername(selectedOfficer.getUsername());
        dispute.setLevel3ApprovalStatus(Dispute.ApprovalStatus.PENDING);
        
        // Update the officer's caseload
        selectedOfficer.setCaseLoad(selectedOfficer.getCaseLoad() + 1);
        caseOfficerRepository.save(selectedOfficer);
        
        logger.info("Successfully assigned Level 3 approver: {} to dispute: {}", 
            selectedOfficer.getUsername(), dispute.getCaseId());
        
        return true;
    }
    
    @Transactional
    public void releaseApprover(String disputeId, int level) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(disputeId);
        
        if (!disputeOpt.isPresent()) {
            logger.warn("Cannot release approver for non-existent dispute: {}", disputeId);
            return;
        }
        
        Dispute dispute = disputeOpt.get();
        String approverUsername = null;
        
        switch (level) {
            case 1:
                approverUsername = dispute.getLevel1ApproverUsername();
                dispute.setLevel1ApproverUsername(null);
                break;
            case 2:
                approverUsername = dispute.getLevel2ApproverUsername();
                dispute.setLevel2ApproverUsername(null);
                break;
            case 3:
                approverUsername = dispute.getLevel3ApproverUsername();
                dispute.setLevel3ApproverUsername(null);
                break;
            default:
                logger.warn("Invalid approval level: {}", level);
                return;
        }
        
        if (approverUsername != null) {
            Optional<CaseOfficer> officerOpt = caseOfficerRepository.findByUsername(approverUsername);
            
            if (officerOpt.isPresent()) {
                CaseOfficer officer = officerOpt.get();
                if (officer.getCaseLoad() > 0) {
                    officer.setCaseLoad(officer.getCaseLoad() - 1);
                    caseOfficerRepository.save(officer);
                }
                
                logger.info("Released Level {} approver: {} from dispute: {}", 
                    level, approverUsername, disputeId);
            }
        }
        
        disputeRepository.save(dispute);
    }
} 