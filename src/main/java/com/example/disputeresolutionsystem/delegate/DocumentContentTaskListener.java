package com.example.disputeresolutionsystem.delegate;

import com.example.disputeresolutionsystem.model.Dispute;
import com.example.disputeresolutionsystem.model.Document;
import com.example.disputeresolutionsystem.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentContentTaskListener implements TaskListener {

    private final DisputeRepository disputeRepository;

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            // Get case ID from task variables
            String caseId = (String) delegateTask.getVariable("caseId");
            if (caseId == null) {
                log.warn("No case ID found in task variables");
                delegateTask.setVariable("documentContent", "No case ID found");
                return;
            }

            // Find the dispute
            Dispute dispute = disputeRepository.findById(caseId).orElse(null);
            if (dispute == null) {
                log.warn("Dispute not found with ID: {}", caseId);
                delegateTask.setVariable("documentContent", "Dispute not found");
                return;
            }

            // Check if dispute has documents
            List<Document> documents = dispute.getDocuments();
            if (documents == null || documents.isEmpty()) {
                log.info("No documents found for dispute: {}", caseId);
                delegateTask.setVariable("documentContent", "No documents attached to this dispute");
                return;
            }

            // Get the first document
            Document document = documents.get(0);
            
            // Read document content
            try {
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    String content = new String(Files.readAllBytes(filePath));
                    
                    // Create a concise version of the document content with file info
                    String documentInfo = "File: " + document.getOriginalFilename() + 
                                        " (" + formatFileSize(document.getFileSize()) + ")\n\n" + 
                                        content;
                    
                    // Limit size if needed
                    if (documentInfo.length() > 8000) {
                        documentInfo = documentInfo.substring(0, 8000) + 
                                      "\n\n... (content truncated due to size limits)";
                    }
                    
                    delegateTask.setVariable("documentContent", documentInfo);
                    log.info("Document content loaded for dispute: {}", caseId);
                } else {
                    log.warn("Document file not found: {}", document.getFilePath());
                    delegateTask.setVariable("documentContent", "Document file not found on server");
                }
            } catch (Exception e) {
                log.error("Error reading document file", e);
                delegateTask.setVariable("documentContent", "Error reading document: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error in document content task listener", e);
            delegateTask.setVariable("documentContent", "Error loading document content: " + e.getMessage());
        }
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
    }
} 