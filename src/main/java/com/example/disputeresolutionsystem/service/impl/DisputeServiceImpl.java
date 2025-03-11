package com.example.disputeresolutionsystem.service.impl;

import com.example.disputeresolutionsystem.dto.DisputeSubmissionRequest;
import com.example.disputeresolutionsystem.dto.DisputeSubmissionResponse;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.Document;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This implementation is no longer needed as DisputeService is now a concrete class.
 * This class is kept for reference but should be removed or refactored.
 * @deprecated Use {@link com.example.disputeresolutionsystem.service.DisputeService} instead.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated
public class DisputeServiceImpl {

    private final DisputeRepository disputeRepository;
    private final RuntimeService runtimeService;

    @Value("${app.document.upload-dir}")
    private String uploadDir;

    /**
     * This method is kept for reference but should be removed or refactored.
     * @deprecated Use {@link com.example.disputeresolutionsystem.service.DisputeService#createDispute} instead.
     */
    @Deprecated
    public DisputeSubmissionResponse submitDispute(DisputeSubmissionRequest request) {
        throw new UnsupportedOperationException("This implementation is deprecated. Use DisputeService.createDispute instead.");
    }

    private String generateCaseId() {
        return "DRS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void processDocuments(DisputeSubmissionRequest request, Dispute dispute) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        dispute.setDocuments(new ArrayList<>());

        for (MultipartFile file : request.getDocuments()) {
            if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
                throw new RuntimeException("File size exceeds 5MB limit: " + file.getOriginalFilename());
            }

            String storedFilename = dispute.getCaseId() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(storedFilename);

            // Save file to disk
            Files.copy(file.getInputStream(), filePath);

            // Create document entity
            Document document = new Document();
            document.setOriginalFilename(file.getOriginalFilename());
            document.setStoredFilename(storedFilename);
            document.setFilePath(filePath.toString());
            document.setFileSize(file.getSize());
            document.setDispute(dispute);

            dispute.getDocuments().add(document);
        }
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