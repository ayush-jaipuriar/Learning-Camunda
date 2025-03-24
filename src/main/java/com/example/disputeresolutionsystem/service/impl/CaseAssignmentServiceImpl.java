package com.example.disputeresolutionsystem.service.impl;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.CaseAssignmentService;
import com.example.disputeresolutionsystem.service.NotificationService;
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
    private final NotificationService notificationService;
    
    @Value("${app.dispute.escalation-hours:4}")
    private double escalationHours;
    
    // Add SLA related settings (minutes for testing purpose)
    @Value("${app.dispute.sla.reminder-threshold-minutes:2}")
    private int reminderThresholdMinutes;
    
    @Value("${app.dispute.sla.compliance-report-threshold-minutes:10}")
    private int complianceReportThresholdMinutes;
    
    @Value("${app.dispute.sla.max-reminders:3}")
    private int maxReminders;
    
    @Override
    @Transactional
    public CaseOfficer assignDisputeToOfficer(Dispute dispute) {
        CaseOfficer.OfficerLevel requiredLevel = determineRequiredOfficerLevel(dispute);
        
        // Find available officers of the required level
        String roleForLevel = getRoleForOfficerLevel(requiredLevel);
        List<CaseOfficer> availableOfficers = caseOfficerRepository.findByRoleAndAvailableTrue(roleForLevel);
        
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
        log.info("Attempting to escalate dispute: {}", dispute.getCaseId());
        
        // Log details of the escalation
        log.info("Dispute {} state before escalation: status={}, assignedOfficer={}", 
                dispute.getCaseId(), 
                dispute.getStatus(),
                dispute.getAssignedOfficer() != null ? dispute.getAssignedOfficer().getUsername() : "none");
        
        // Update dispute status
        dispute.setEscalated(true);
        dispute.setEscalationTimestamp(LocalDateTime.now());
        dispute.setPriorityLevel(Dispute.PriorityLevel.HIGH); // Increase priority
        
        // Find an available supervisor
        Optional<CaseOfficer> supervisor = caseOfficerRepository.findByRoleAndAvailableTrue("SUPERVISOR")
                .stream()
                .findFirst();
        
        if (supervisor.isPresent()) {
            // Get the previously assigned officer
            CaseOfficer previousOfficer = dispute.getAssignedOfficer();
            
            // Update workload for the previous officer if one was assigned
            if (previousOfficer != null) {
                previousOfficer.setCurrentWorkload(previousOfficer.getCurrentWorkload() - 1);
                caseOfficerRepository.save(previousOfficer);
            }
            
            // Assign to supervisor
            dispute.setAssignedOfficer(supervisor.get());
            dispute.setAssignmentTimestamp(LocalDateTime.now());
            dispute.setStatus("Escalated");
            
            // Update supervisor's workload
            CaseOfficer supervisorOfficer = supervisor.get();
            supervisorOfficer.setCurrentWorkload(supervisorOfficer.getCurrentWorkload() + 1);
            caseOfficerRepository.save(supervisorOfficer);
            
            // Send escalation notification
            notificationService.sendEscalationNotification(dispute, supervisorOfficer);
            
            log.info("Dispute {} escalated and assigned to supervisor {}", dispute.getCaseId(), supervisorOfficer.getUsername());
        } else {
            dispute.setStatus("Escalated - Pending Assignment");
            log.warn("No supervisor available for escalated dispute: {}", dispute.getCaseId());
        }
        
        disputeRepository.save(dispute);
        return true;
    }
    
    @Override
    @Scheduled(fixedRate = 30 * 1000) // Run every 30 seconds for testing
    @Transactional
    public void monitorSLAViolations() {
        log.info("Checking for SLA violations and sending reminders...");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find active disputes (not resolved)
        List<Dispute> activeDisputes = disputeRepository.findByStatusNot("Resolved");
        
        int remindersCount = 0;
        int violationsCount = 0;
        int complianceReportsCount = 0;
        
        for (Dispute dispute : activeDisputes) {
            // Skip disputes that don't have an SLA deadline set
            if (dispute.getSlaDeadline() == null) {
                continue;
            }
            
            long minutesUntilDeadline = ChronoUnit.MINUTES.between(now, dispute.getSlaDeadline());
            
            // Case 1: Approaching deadline - send reminders
            if (minutesUntilDeadline > 0 && minutesUntilDeadline <= reminderThresholdMinutes && 
                    dispute.getRemindersSent() < maxReminders) {
                
                boolean reminderSent = notificationService.sendSLAReminderNotification(dispute, (int) minutesUntilDeadline);
                if (reminderSent) {
                    remindersCount++;
                    disputeRepository.save(dispute); // Save the updated reminder count
                }
            }
            // Case 2: SLA violated but no compliance report needed yet
            else if (minutesUntilDeadline < 0 && !dispute.isEscalated()) {
                // Send SLA violation notification
                notificationService.sendSLAViolationNotification(dispute);
                
                // Escalate the dispute
                escalateDispute(dispute);
                violationsCount++;
            }
            // Case 3: Severe SLA violation - generate compliance report
            else if (minutesUntilDeadline < -complianceReportThresholdMinutes && 
                    !dispute.isComplianceReportGenerated()) {
                
                boolean reportGenerated = notificationService.generateComplianceReport(dispute);
                if (reportGenerated) {
                    complianceReportsCount++;
                    disputeRepository.save(dispute); // Save the updated compliance report flag
                }
            }
        }
        
        log.info("SLA monitoring completed: {} reminders sent, {} violations processed, {} compliance reports generated",
                remindersCount, violationsCount, complianceReportsCount);
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
    
    // Helper method to map officer level to role
    private String getRoleForOfficerLevel(CaseOfficer.OfficerLevel level) {
        switch (level) {
            case SUPERVISOR:
                return "SUPERVISOR";
            case SENIOR:
                return "SENIOR_OFFICER";
            case LEVEL_1:
            default:
                return "OFFICER";
        }
    }
} 