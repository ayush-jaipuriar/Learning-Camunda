package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.dto.DisputeSubmissionRequest;
import com.example.disputeresolutionsystem.dto.DisputeSubmissionResponse;
import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.Document;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import com.example.disputeresolutionsystem.service.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
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

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DisputeSubmissionResponse> submitDispute(
            @RequestParam("userId") String userId,
            @RequestParam("disputeType") String disputeType,
            @RequestParam("creditReportId") String creditReportId,
            @RequestParam(value = "documents", required = false) MultipartFile document) {
        
        log.info("Received dispute submission request for user: {}", userId);
        
        DisputeSubmissionRequest request = new DisputeSubmissionRequest();
        request.setUserId(userId);
        request.setDisputeType(disputeType);
        request.setCreditReportId(creditReportId);
        
        // Handle the document if provided
        if (document != null && !document.isEmpty()) {
            request.setDocuments(Collections.singletonList(document));
        } else {
            request.setDocuments(Collections.emptyList());
        }
        
        DisputeSubmissionResponse response = disputeService.submitDispute(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<Dispute>> getAllDisputes() {
        List<Dispute> disputes = disputeRepository.findAll();
        return ResponseEntity.ok(disputes);
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<Dispute> getDisputeById(@PathVariable String caseId) {
        Dispute dispute = disputeRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Dispute not found with ID: " + caseId));
        return ResponseEntity.ok(dispute);
    }
    
    @GetMapping("/{caseId}/documents")
    public ResponseEntity<Map<String, Object>> getDisputeDocuments(@PathVariable String caseId) {
        Dispute dispute = disputeRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Dispute not found with ID: " + caseId));
        
        Map<String, Object> response = new HashMap<>();
        
        if (dispute.getDocuments() == null || dispute.getDocuments().isEmpty()) {
            response.put("hasDocuments", false);
            return ResponseEntity.ok(response);
        }
        
        Document document = dispute.getDocuments().get(0); // Get the first document
        response.put("hasDocuments", true);
        response.put("documentId", document.getId());
        response.put("originalFilename", document.getOriginalFilename());
        response.put("fileSize", document.getFileSize());
        
        try {
            // Read file content
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                String content = new String(Files.readAllBytes(filePath));
                response.put("content", content);
            } else {
                response.put("content", "File not found on server");
            }
        } catch (IOException e) {
            log.error("Error reading document file", e);
            response.put("content", "Error reading file: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
} 