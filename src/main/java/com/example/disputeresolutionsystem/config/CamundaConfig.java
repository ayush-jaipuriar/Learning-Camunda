package com.example.disputeresolutionsystem.config;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.repository.CaseOfficerRepository;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class CamundaConfig {

    @Autowired
    private IdentityService identityService;
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @Autowired
    private CaseOfficerRepository caseOfficerRepository;

    @Value("${camunda.bpm.admin-user.id:admin}")
    private String adminUserId;

    @Value("${camunda.bpm.admin-user.password:admin}")
    private String adminPassword;

    @Value("${camunda.bpm.admin-user.first-name:Admin}")
    private String adminFirstName;

    @Value("${camunda.bpm.admin-user.last-name:Admin}")
    private String adminLastName;

    @Bean
    public String setupCamundaUsers(ProcessEngine processEngine) {
        // Create admin user if it doesn't exist
        try {
            log.info("Checking if admin user exists...");
            User adminUser = identityService.createUserQuery()
                .userId(adminUserId)
                .singleResult();

            if (adminUser == null) {
                log.info("Creating admin user: {}", adminUserId);
                User newAdminUser = identityService.newUser(adminUserId);
                newAdminUser.setFirstName(adminFirstName);
                newAdminUser.setLastName(adminLastName);
                newAdminUser.setEmail("admin@example.com");
                newAdminUser.setPassword(adminPassword);
                identityService.saveUser(newAdminUser);

                // Check if camunda-admin group exists
                Group adminGroup = identityService.createGroupQuery()
                    .groupId("camunda-admin")
                    .singleResult();

                if (adminGroup == null) {
                    log.info("Creating camunda-admin group");
                    Group newAdminGroup = identityService.newGroup("camunda-admin");
                    newAdminGroup.setName("Camunda Administrators");
                    newAdminGroup.setType("SYSTEM");
                    identityService.saveGroup(newAdminGroup);

                    // Create admin authorization
                    Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
                    authorization.setGroupId("camunda-admin");
                    authorization.addPermission(Permissions.ALL);
                    authorization.setResource(Resources.APPLICATION);
                    authorization.setResourceId("*");
                    authorizationService.saveAuthorization(authorization);
                }

                try {
                    // Check if the admin user is already a member of the camunda-admin group
                    List<Group> userGroups = identityService.createGroupQuery()
                        .groupMember(adminUserId)
                        .list();
                    boolean isAdmin = userGroups.stream()
                        .anyMatch(group -> group.getId().equals("camunda-admin"));
                    if (!isAdmin) {
                        log.info("Adding admin user to camunda-admin group");
                        identityService.createMembership(adminUserId, "camunda-admin");
                    } else {
                        log.info("Admin user already in camunda-admin group");
                    }
                } catch (Exception e) {
                    log.error("Error checking/creating admin membership: {}", e.getMessage());
                    try {
                        // Fallback - try to create membership directly
                        identityService.createMembership(adminUserId, "camunda-admin");
                        log.info("Created admin membership as fallback");
                    } catch (Exception ex) {
                        log.error("Failed to create admin membership: {}", ex.getMessage());
                    }
                }
            } else {
                log.info("Admin user already exists: {}", adminUserId);
            }

            // Sync case officers from database to Camunda
            syncCaseOfficers();

        } catch (Exception e) {
            log.error("Error setting up Camunda users", e);
        }
        
        return "Camunda users setup completed";
    }
    
    private void syncCaseOfficers() {
        log.info("Syncing case officers with Camunda users...");
        List<CaseOfficer> officers = caseOfficerRepository.findAll();
        if (officers.isEmpty()) {
            log.warn("No case officers found in database to sync with Camunda");
            return;
        }
        
        for (CaseOfficer officer : officers) {
            String username = officer.getUsername();
            
            // Check if Camunda user exists
            User camundaUser = identityService.createUserQuery()
                .userId(username)
                .singleResult();
            
            if (camundaUser == null) {
                log.info("Creating Camunda user for officer: {}", username);
                User newUser = identityService.newUser(username);
                newUser.setFirstName(officer.getUsername());
                newUser.setLastName("");
                newUser.setEmail(username + "@example.com");
                newUser.setPassword(username);
                identityService.saveUser(newUser);
                
                // Add user to appropriate group based on level
                String groupId = "";
                
                switch (officer.getLevel()) {
                    case LEVEL_1:
                        groupId = "level_1_officers";
                        break;
                    case SENIOR:
                        groupId = "senior_officers";
                        break;
                    case SUPERVISOR:
                        groupId = "supervisors";
                        break;
                }
                
                // Create group if it doesn't exist
                Group group = identityService.createGroupQuery()
                    .groupId(groupId)
                    .singleResult();
                
                if (group == null) {
                    log.info("Creating group: {}", groupId);
                    Group newGroup = identityService.newGroup(groupId);
                    newGroup.setName(groupId.replace("_", " ").toUpperCase());
                    newGroup.setType("WORKFLOW");
                    identityService.saveGroup(newGroup);
                }
                
                // Add user to group
                try {
                    identityService.createMembership(username, groupId);
                    log.info("Added user {} to group {}", username, groupId);
                } catch (Exception e) {
                    log.error("Error adding user {} to group {}: {}", username, groupId, e.getMessage());
                }
            }
        }
        
        log.info("Case officer sync completed");
    }
} 