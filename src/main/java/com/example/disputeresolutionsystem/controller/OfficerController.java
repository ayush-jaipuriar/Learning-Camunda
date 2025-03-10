package com.example.disputeresolutionsystem.controller;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/officers")
@RequiredArgsConstructor
public class OfficerController {

    private final CaseOfficerRepository caseOfficerRepository;

    @GetMapping
    public ResponseEntity<List<CaseOfficer>> getAllOfficers() {
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        return ResponseEntity.ok(officers);
    }

    @GetMapping("/workloads")
    public ResponseEntity<List<Map<String, Object>>> getOfficerWorkloads() {
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        
        List<Map<String, Object>> workloads = officers.stream()
                .map(officer -> {
                    Map<String, Object> workload = new HashMap<>();
                    workload.put("id", officer.getId());
                    workload.put("username", officer.getUsername());
                    workload.put("fullName", officer.getFullName());
                    workload.put("level", officer.getLevel());
                    workload.put("currentWorkload", officer.getCurrentWorkload());
                    workload.put("maxWorkload", officer.getMaxWorkload());
                    workload.put("availableCapacity", officer.getMaxWorkload() - officer.getCurrentWorkload());
                    return workload;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(workloads);
    }
} 