package com.example.disputeresolutionsystem.service.impl;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.CaseAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseAssignmentServiceImpl implements CaseAssignmentService {

    private final DisputeRepository disputeRepository;
    private final CaseOfficerRepository caseOfficerRepository;
    
    @Value("${app.dispute.escalation-hours:4}")
    private double escalationHours;
    
    @Override
    @Transactional
    public CaseOfficer assignDisputeToOfficer(Dispute dispute) {
        CaseOfficer.OfficerLevel requiredLevel = determineRequiredOfficerLevel(dispute);
        
        // Find available officers of the required level
        List<CaseOfficer> availableOfficers = caseOfficerRepository.findAvailableOfficersByLevel(requiredLevel);
        
        if (availableOfficers.isEmpty()) {
            log.warn("No available officers of level {} found for dispute {}", requiredLevel, dispute.getCaseId());
            return null;
        }
        
        // Get the officer with the lowest workload
        CaseOfficer selectedOfficer = availableOfficers.get(0);
        
        // Update the dispute with assignment information
        dispute.setAssignedOfficer(selectedOfficer);
        dispute.setAssignmentTimestamp(LocalDateTime.now());
        dispute.setStatus("Assigned");
        
        // Update the officer's workload
        selectedOfficer.setCurrentWorkload(selectedOfficer.getCurrentWorkload() + 1);
        
        // Save both entities
        caseOfficerRepository.save(selectedOfficer);
        disputeRepository.save(dispute);
        
        log.info("Dispute {} assigned to officer {}", dispute.getCaseId(), selectedOfficer.getUsername());
        
        return selectedOfficer;
    }
    
    @Override
    @Scheduled(fixedRate = 60 * 1000) // Run every minute for testing
    @Transactional
    public void checkForEscalations() {
        log.info("Checking for disputes that need escalation...");
        
        // Convert hours to a time duration
        long escalationMinutes = (long)(escalationHours * 60);
        LocalDateTime escalationThreshold = LocalDateTime.now().minusMinutes(escalationMinutes);
        
        // Find disputes that are submitted but not assigned and older than the threshold
        List<Dispute> disputesToEscalate = disputeRepository.findByStatusAndSubmissionTimestampBefore("Submitted", escalationThreshold);
        
        for (Dispute dispute : disputesToEscalate) {
            escalateDispute(dispute);
        }
        
        log.info("Escalation check completed. {} disputes were escalated.", disputesToEscalate.size());
    }
    
    @Override
    @Transactional
    public boolean escalateDispute(Dispute dispute) {
        log.info("Escalating dispute: {}", dispute.getCaseId());
        
        // Update dispute status
        dispute.setEscalated(true);
        dispute.setEscalationTimestamp(LocalDateTime.now());
        dispute.setPriorityLevel(Dispute.PriorityLevel.HIGH); // Increase priority
        
        // Try to find a supervisor
        Optional<CaseOfficer> supervisor = caseOfficerRepository.findAvailableOfficersByLevel(CaseOfficer.OfficerLevel.SUPERVISOR)
                .stream()
                .findFirst();
        
        if (supervisor.isPresent()) {
            CaseOfficer supervisorOfficer = supervisor.get();
            
            // If dispute was already assigned, decrease previous officer's workload
            if (dispute.getAssignedOfficer() != null) {
                CaseOfficer previousOfficer = dispute.getAssignedOfficer();
                previousOfficer.setCurrentWorkload(previousOfficer.getCurrentWorkload() - 1);
                caseOfficerRepository.save(previousOfficer);
            }
            
            // Assign to supervisor
            dispute.setAssignedOfficer(supervisorOfficer);
            dispute.setAssignmentTimestamp(LocalDateTime.now());
            dispute.setStatus("Escalated");
            
            // Update supervisor's workload
            supervisorOfficer.setCurrentWorkload(supervisorOfficer.getCurrentWorkload() + 1);
            caseOfficerRepository.save(supervisorOfficer);
            
            log.info("Dispute {} escalated and assigned to supervisor {}", dispute.getCaseId(), supervisorOfficer.getUsername());
        } else {
            dispute.setStatus("Escalated - Pending Assignment");
            log.warn("No supervisor available for escalated dispute: {}", dispute.getCaseId());
        }
        
        disputeRepository.save(dispute);
        return true;
    }
    
    private CaseOfficer.OfficerLevel determineRequiredOfficerLevel(Dispute dispute) {
        // Determine the required officer level based on dispute complexity and priority
        if (dispute.getComplexityLevel() == Dispute.ComplexityLevel.HIGH_RISK) {
            return CaseOfficer.OfficerLevel.SUPERVISOR;
        } else if (dispute.getComplexityLevel() == Dispute.ComplexityLevel.COMPLEX) {
            return CaseOfficer.OfficerLevel.SENIOR;
        } else {
            return CaseOfficer.OfficerLevel.LEVEL_1;
        }
    }
} 