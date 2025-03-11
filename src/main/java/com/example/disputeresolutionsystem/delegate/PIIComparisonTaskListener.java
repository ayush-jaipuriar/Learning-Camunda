package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.dto.PIIComparisonDTO;
import com.example.disputeresolutionsystem.service.PIIComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * Task listener that populates PII comparison data in the Camunda task form
 */
@Slf4j
@Component("piiComparisonTaskListener")
@RequiredArgsConstructor
public class PIIComparisonTaskListener implements TaskListener {

    private final PIIComparisonService piiComparisonService;

    @Override
    public void notify(DelegateTask delegateTask) {
        // Get case ID from process variables
        String caseId = (String) delegateTask.getVariable("caseId");
        if (caseId == null) {
            log.warn("No case ID found in task variables");
            return;
        }
        
        log.info("Fetching PII comparison data for dispute: {}", caseId);
        
        // Get PII comparison data
        PIIComparisonDTO comparison = piiComparisonService.comparePIIData(caseId);
        if (comparison == null) {
            log.warn("No PII comparison data found for dispute: {}", caseId);
            return;
        }
        
        // Set task variables for PII comparison
        delegateTask.setVariable("piiValidationStatus", comparison.getValidationStatus().toString());
        
        // Submitted user data
        delegateTask.setVariable("submittedUserFullName", comparison.getSubmittedUserFullName());
        delegateTask.setVariable("submittedUserAddress", comparison.getSubmittedUserAddress());
        delegateTask.setVariable("submittedUserPhoneNumber", comparison.getSubmittedUserPhoneNumber());
        delegateTask.setVariable("submittedUserEmailAddress", comparison.getSubmittedUserEmailAddress());
        
        // Database user data
        delegateTask.setVariable("databaseUserFullName", comparison.getDatabaseUserFullName());
        delegateTask.setVariable("databaseUserAddress", comparison.getDatabaseUserAddress());
        delegateTask.setVariable("databaseUserPhoneNumber", comparison.getDatabaseUserPhoneNumber());
        delegateTask.setVariable("databaseUserEmailAddress", comparison.getDatabaseUserEmailAddress());
        
        // Match indicators
        delegateTask.setVariable("fullNameMatch", comparison.isFullNameMatch());
        delegateTask.setVariable("addressMatch", comparison.isAddressMatch());
        delegateTask.setVariable("phoneNumberMatch", comparison.isPhoneNumberMatch());
        delegateTask.setVariable("emailMatch", comparison.isEmailMatch());
        
        log.info("Set PII comparison data for task: {}", delegateTask.getId());
    }
} 