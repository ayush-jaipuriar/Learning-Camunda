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

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final RuntimeService runtimeService;

    @Value("${app.document.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public DisputeSubmissionResponse submitDispute(DisputeSubmissionRequest request) {
        try {
            // Generate case ID
            String caseId = generateCaseId();

            // Create dispute entity
            Dispute dispute = new Dispute();
            dispute.setCaseId(caseId);
            dispute.setUserId(request.getUserId());
            dispute.setDisputeType(request.getDisputeType());
            dispute.setCreditReportId(request.getCreditReportId());
            dispute.setPriorityLevel("MEDIUM"); // Default priority

            // Process documents
            processDocuments(request, dispute);

            // Save dispute
            disputeRepository.save(dispute);

            // Try to start Camunda process
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("caseId", caseId);
                variables.put("userId", request.getUserId());
                variables.put("disputeType", request.getDisputeType());
                variables.put("creditReportId", request.getCreditReportId());

                runtimeService.startProcessInstanceByKey("dispute_resolution_process", caseId, variables);
                log.info("Camunda process started successfully for case ID: {}", caseId);
            } catch (Exception e) {
                log.warn("Failed to start Camunda process for case ID: {}. Error: {}", caseId, e.getMessage());
                // Continue with dispute creation even if Camunda process fails
            }

            log.info("Dispute case created successfully with ID: {}", caseId);

            return DisputeSubmissionResponse.builder()
                    .caseId(caseId)
                    .status("Submitted")
                    .message("Dispute case created successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error processing dispute submission", e);
            throw new RuntimeException("Failed to process dispute submission: " + e.getMessage());
        }
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
} 