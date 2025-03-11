package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.dto.DisputeSubmissionDTO;
import com.example.disputeresolutionsystem.dto.DisputeSubmissionRequest;
import com.example.disputeresolutionsystem.dto.DisputeSubmissionResponse;
import com.example.disputeresolutionsystem.dto.PIIComparisonDTO;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.Dispute.PIIValidationStatus;
import com.example.disputeresolutionsystem.model.Document;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.DisputeService;
import com.example.disputeresolutionsystem.service.PIIComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final DisputeRepository disputeRepository;
    private final PIIComparisonService piiComparisonService;

    @GetMapping
    public ResponseEntity<List<Dispute>> getAllDisputes() {
        List<Dispute> disputes = disputeRepository.findAll();
        return ResponseEntity.ok(disputes);
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<Dispute> getDisputeById(@PathVariable String caseId) {
        return disputeRepository.findById(caseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{caseId}/documents")
    public ResponseEntity<Map<String, Object>> getDisputeDocuments(@PathVariable String caseId) {
        Map<String, Object> response = new HashMap<>();
        
        Dispute dispute = disputeRepository.findById(caseId).orElse(null);
        if (dispute == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (dispute.getDocuments() == null || dispute.getDocuments().isEmpty()) {
            response.put("hasDocuments", false);
            return ResponseEntity.ok(response);
        }
        
        Document document = dispute.getDocuments().get(0); // Get the first document
        
        response.put("documentId", document.getId());
        response.put("originalFilename", document.getOriginalFilename());
        response.put("fileSize", document.getFileSize());
        
        try {
            // Read file content
            Path filePath = Paths.get(document.getFilePath());
            byte[] fileContent = Files.readAllBytes(filePath);
            
            response.put("hasDocuments", true);
            response.put("fileContent", fileContent);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error reading document file", e);
            response.put("error", "Failed to read document file");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Submit a new dispute with PII information
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitDispute(
            @RequestPart("dispute") DisputeSubmissionDTO disputeSubmission,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {
        
        log.info("Received dispute submission for user: {}", disputeSubmission.getUserId());
        
        // Create the dispute with PII information
        Dispute dispute = disputeService.createDispute(disputeSubmission, documents);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Dispute submitted successfully");
        response.put("caseId", dispute.getCaseId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get PII comparison data for a dispute
     */
    @GetMapping("/{caseId}/pii-comparison")
    public ResponseEntity<PIIComparisonDTO> getPIIComparison(@PathVariable String caseId) {
        log.info("Getting PII comparison for dispute: {}", caseId);
        
        PIIComparisonDTO comparison = piiComparisonService.comparePIIData(caseId);
        if (comparison == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(comparison);
    }
    
    /**
     * Update PII validation status
     */
    @PostMapping("/{caseId}/pii-validation")
    public ResponseEntity<Map<String, String>> updatePIIValidation(
            @PathVariable String caseId,
            @RequestParam PIIValidationStatus status,
            @RequestParam(required = false) String notes) {
        
        log.info("Updating PII validation for dispute {}: {}", caseId, status);
        
        piiComparisonService.updatePIIValidationStatus(caseId, status, notes);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "PII validation status updated successfully");
        
        return ResponseEntity.ok(response);
    }
} 