package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.dto.DisputeSubmissionDTO;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.Document;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final RuntimeService runtimeService;
    private final String uploadDir = "uploads";

    /**
     * Create a new dispute with PII information
     */
    @Transactional
    public Dispute createDispute(DisputeSubmissionDTO submission, List<MultipartFile> files) {
        log.info("Creating dispute for user: {}", submission.getUserId());
        
        // Generate a unique case ID
        String caseId = "CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Dispute dispute = new Dispute();
        dispute.setCaseId(caseId);
        dispute.setUserId(submission.getUserId());
        dispute.setDisputeType(submission.getDisputeType());
        dispute.setCreditReportId(submission.getCreditReportId());
        dispute.setStatus("Submitted");
        
        // Set PII information
        dispute.setSubmittedUserFullName(submission.getUserFullName());
        dispute.setSubmittedUserAddress(submission.getUserAddress());
        dispute.setSubmittedUserPhoneNumber(submission.getUserPhoneNumber());
        dispute.setSubmittedUserEmailAddress(submission.getUserEmailAddress());
        dispute.setDescription(submission.getDescription());
        
        // Set initial PII validation status
        dispute.setPiiValidationStatus(Dispute.PIIValidationStatus.PENDING);
        
        // Assess complexity and priority (simple implementation)
        assessDisputeComplexity(dispute);
        
        // Save the dispute first to get the ID
        disputeRepository.save(dispute);
        
        // Process any attached documents
        if (files != null && !files.isEmpty()) {
            List<Document> documents = new ArrayList<>();
            
            for (MultipartFile file : files) {
                try {
                    // Create uploads directory if it doesn't exist
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    
                    // Generate a unique filename
                    String originalFilename = file.getOriginalFilename();
                    String storedFilename = UUID.randomUUID().toString() + 
                            (originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "");
                    
                    // Save the file
                    Path filePath = uploadPath.resolve(storedFilename);
                    Files.copy(file.getInputStream(), filePath);
                    
                    // Create document record
                    Document document = new Document();
                    document.setOriginalFilename(originalFilename);
                    document.setStoredFilename(storedFilename);
                    document.setFilePath(filePath.toString());
                    document.setFileSize(file.getSize());
                    document.setDispute(dispute);
                    
                    documents.add(document);
                    
                } catch (IOException e) {
                    log.error("Failed to store file: {}", file.getOriginalFilename(), e);
                }
            }
            
            dispute.setDocuments(documents);
            disputeRepository.save(dispute);
        }
        
        // Start Camunda process instance
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("caseId", caseId);
        processVariables.put("userId", dispute.getUserId());
        processVariables.put("disputeType", dispute.getDisputeType());
        processVariables.put("creditReportId", dispute.getCreditReportId());
        processVariables.put("complexityLevel", dispute.getComplexityLevel().toString());
        processVariables.put("priorityLevel", dispute.getPriorityLevel().toString());
        
        // Start the process instance
        runtimeService.startProcessInstanceByKey("dispute_resolution_process", caseId, processVariables);
        log.info("Started Camunda process for dispute with case ID: {}", caseId);
        
        log.info("Created dispute with case ID: {}", caseId);
        return dispute;
    }
    
    /**
     * Assess the complexity and priority of a dispute based on its type and other factors
     */
    private void assessDisputeComplexity(Dispute dispute) {
        // Default values
        Dispute.ComplexityLevel complexityLevel = Dispute.ComplexityLevel.SIMPLE;
        Dispute.PriorityLevel priorityLevel = Dispute.PriorityLevel.MEDIUM;
        
        // Determine complexity based on dispute type
        String disputeType = dispute.getDisputeType().toLowerCase();
        
        if (disputeType.contains("fraud") || disputeType.contains("identity theft")) {
            complexityLevel = Dispute.ComplexityLevel.HIGH_RISK;
            priorityLevel = Dispute.PriorityLevel.HIGH;
        } else if (disputeType.contains("bankruptcy") || disputeType.contains("legal") || 
                   disputeType.contains("complex") || disputeType.contains("multiple")) {
            complexityLevel = Dispute.ComplexityLevel.COMPLEX;
            priorityLevel = Dispute.PriorityLevel.MEDIUM;
        }
        
        // Set the assessed values
        dispute.setComplexityLevel(complexityLevel);
        dispute.setPriorityLevel(priorityLevel);
        
        log.info("Dispute {} assessed as {} complexity with {} priority", 
                dispute.getCaseId(), complexityLevel, priorityLevel);
    }
} 