package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.dto.DisputeSubmissionRequest;
import com.example.disputeresolutionsystem.dto.DisputeSubmissionResponse;

public interface DisputeService {
    DisputeSubmissionResponse submitDispute(DisputeSubmissionRequest request);
} 