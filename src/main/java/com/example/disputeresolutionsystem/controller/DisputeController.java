package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.dto.DisputeSubmissionRequest;
import com.example.disputeresolutionsystem.dto.DisputeSubmissionResponse;
import com.example.disputeresolutionsystem.service.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/submit")
    public ResponseEntity<DisputeSubmissionResponse> submitDispute(@Valid DisputeSubmissionRequest request) {
        log.info("Received dispute submission request for user: {}", request.getUserId());
        DisputeSubmissionResponse response = disputeService.submitDispute(request);
        return ResponseEntity.ok(response);
    }
} 