package com.example.disputeresolutionsystem.config;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import com.example.disputeresolutionsystem.service.CamundaUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CamundaConfig {

    @Value("${camunda.bpm.admin-user.id}")
    private String adminUserId;

    @Value("${camunda.bpm.admin-user.password}")
    private String adminPassword;

    @Value("${camunda.bpm.admin-user.first-name:Admin}")
    private String adminFirstName;

    @Value("${camunda.bpm.admin-user.last-name:Admin}")
    private String adminLastName;

    @Bean
    public CommandLineRunner initAdminUser(IdentityService identityService) {
        return args -> {
            log.info("Checking if admin user exists...");
            
            // Check if admin user exists
            User adminUser = identityService.createUserQuery()
                    .userId(adminUserId)
                    .singleResult();
            
            if (adminUser == null) {
                log.info("Creating admin user: {}", adminUserId);
                
                // Create admin user
                User newUser = identityService.newUser(adminUserId);
                newUser.setFirstName(adminFirstName);
                newUser.setLastName(adminLastName);
                newUser.setPassword(adminPassword);
                newUser.setEmail("admin@example.com");
                identityService.saveUser(newUser);
                
                // Create camunda-admin group if it doesn't exist
                Group adminGroup = identityService.createGroupQuery()
                        .groupId("camunda-admin")
                        .singleResult();
                
                if (adminGroup == null) {
                    log.info("Creating camunda-admin group");
                    Group newGroup = identityService.newGroup("camunda-admin");
                    newGroup.setName("Camunda Admin");
                    newGroup.setType("SYSTEM");
                    identityService.saveGroup(newGroup);
                }
                
                // Add admin user to camunda-admin group
                try {
                    // Check if the user is already a member of the group
                    boolean isMember = identityService.createUserQuery()
                            .userId(adminUserId)
                            .memberOfGroup("camunda-admin")
                            .count() > 0;
                    
                    if (!isMember) {
                        log.info("Adding admin user to camunda-admin group");
                        identityService.createMembership(adminUserId, "camunda-admin");
                    } else {
                        log.info("Admin user already in camunda-admin group");
                    }
                } catch (Exception e) {
                    log.error("Error checking/creating admin membership: {}", e.getMessage());
                    // Create the membership anyway as a fallback
                    try {
                        identityService.createMembership(adminUserId, "camunda-admin");
                        log.info("Created admin membership as fallback");
                    } catch (Exception ex) {
                        log.error("Failed to create admin membership: {}", ex.getMessage());
                    }
                }
            } else {
                log.info("Admin user already exists: {}", adminUserId);
            }
        };
    }
    
    @Bean
    public CommandLineRunner syncCaseOfficers(CaseOfficerRepository caseOfficerRepository, 
                                             CamundaUserService camundaUserService) {
        return args -> {
            log.info("Syncing case officers with Camunda...");
            
            // Get all case officers
            List<CaseOfficer> officers = caseOfficerRepository.findAll();
            
            if (officers.isEmpty()) {
                log.info("No case officers found to sync");
                return;
            }
            
            // Sync officers with Camunda
            int syncCount = camundaUserService.syncOfficersWithCamundaUsers(officers);
            
            log.info("Synced {} case officers with Camunda", syncCount);
        };
    }
} 