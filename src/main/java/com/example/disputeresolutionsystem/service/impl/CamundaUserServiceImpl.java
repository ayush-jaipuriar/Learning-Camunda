package com.example.disputeresolutionsystem.service.impl;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.service.CamundaUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CamundaUserServiceImpl implements CamundaUserService {

    private final IdentityService identityService;

    @Override
    public int syncOfficersWithCamundaUsers(List<CaseOfficer> officers) {
        int count = 0;
        
        // Create or update users for each officer
        for (CaseOfficer officer : officers) {
            if (createCamundaUser(officer)) {
                count++;
            }
        }
        
        log.info("Synchronized {} officers with Camunda users", count);
        return count;
    }

    @Override
    public boolean createCamundaUser(CaseOfficer officer) {
        try {
            // Sanitize username for Camunda
            String camundaUsername = sanitizeUsername(officer.getUsername());
            
            // Check if user already exists
            User existingUser = identityService.createUserQuery()
                    .userId(camundaUsername)
                    .singleResult();
            
            if (existingUser == null) {
                // Create new user
                User newUser = identityService.newUser(camundaUsername);
                newUser.setFirstName(officer.getFullName().split(" ")[0]);
                newUser.setLastName(officer.getFullName().substring(officer.getFullName().indexOf(" ") + 1));
                newUser.setEmail(officer.getEmail());
                newUser.setPassword(camundaUsername); // Default password same as username
                identityService.saveUser(newUser);
                
                log.info("Created Camunda user for officer: {}", camundaUsername);
            } else {
                // Update existing user
                existingUser.setFirstName(officer.getFullName().split(" ")[0]);
                existingUser.setLastName(officer.getFullName().substring(officer.getFullName().indexOf(" ") + 1));
                existingUser.setEmail(officer.getEmail());
                identityService.saveUser(existingUser);
                
                log.info("Updated Camunda user for officer: {}", camundaUsername);
            }
            
            // Assign user to appropriate group based on officer level
            assignUserToGroup(camundaUsername, getGroupIdForOfficerLevel(officer.getLevel()));
            
            return true;
        } catch (Exception e) {
            log.error("Error creating/updating Camunda user for officer: {}", officer.getUsername(), e);
            return false;
        }
    }

    @Override
    public boolean deleteCamundaUser(String username) {
        try {
            // Sanitize username for Camunda
            String camundaUsername = sanitizeUsername(username);
            identityService.deleteUser(camundaUsername);
            log.info("Deleted Camunda user: {}", camundaUsername);
            return true;
        } catch (Exception e) {
            log.error("Error deleting Camunda user: {}", username, e);
            return false;
        }
    }
    
    private void assignUserToGroup(String username, String groupId) {
        // Sanitize the group ID to ensure it's valid for Camunda
        String sanitizedGroupId = sanitizeGroupId(groupId);
        
        // Check if group exists, create if not
        Group group = identityService.createGroupQuery()
                .groupId(sanitizedGroupId)
                .singleResult();
        
        if (group == null) {
            group = identityService.newGroup(sanitizedGroupId);
            group.setName(groupId); // Keep the original name for display purposes
            group.setType("WORKFLOW");
            identityService.saveGroup(group);
            log.info("Created Camunda group: {}", sanitizedGroupId);
        }
        
        // Check if user is already in group
        if (!identityService.createUserQuery()
                .userId(username)
                .memberOfGroup(sanitizedGroupId)
                .list()
                .isEmpty()) {
            return; // User already in group
        }
        
        // Add user to group
        identityService.createMembership(username, sanitizedGroupId);
        log.info("Added user {} to group {}", username, sanitizedGroupId);
    }
    
    /**
     * Sanitize group ID to ensure it's valid for Camunda
     * Camunda resource identifiers must follow these rules:
     * - Must start with a letter or underscore
     * - Can only contain letters, numbers, and underscores
     * - Cannot contain special characters or spaces
     */
    private String sanitizeGroupId(String groupId) {
        // First, replace special characters with nothing (just use alphanumeric)
        String sanitized = groupId.replaceAll("[^a-zA-Z0-9]", "");
        
        // Ensure the ID starts with a letter (if it starts with a number, prepend 'group')
        if (sanitized.length() > 0 && !Character.isLetter(sanitized.charAt(0))) {
            sanitized = "group" + sanitized;
        }
        
        // Ensure the ID is not empty
        if (sanitized.isEmpty()) {
            sanitized = "group" + System.currentTimeMillis();
        }
        
        return sanitized;
    }
    
    /**
     * Sanitize username to ensure it's valid for Camunda
     * Camunda resource identifiers must follow these rules:
     * - Must start with a letter or underscore
     * - Can only contain letters, numbers, and underscores
     * - Cannot contain special characters or spaces
     */
    private String sanitizeUsername(String username) {
        // First, replace dots and other special characters with underscores
        String sanitized = username.replaceAll("[^a-zA-Z0-9]", "_");
        
        // Ensure the ID starts with a letter (if it starts with a number, prepend 'user_')
        if (sanitized.length() > 0 && !Character.isLetter(sanitized.charAt(0)) && sanitized.charAt(0) != '_') {
            sanitized = "user_" + sanitized;
        }
        
        // Ensure the ID is not empty
        if (sanitized.isEmpty()) {
            sanitized = "user_" + System.currentTimeMillis();
        }
        
        return sanitized;
    }
    
    @Override
    public String getSanitizedUsername(String username) {
        return sanitizeUsername(username);
    }
    
    private String getGroupIdForOfficerLevel(CaseOfficer.OfficerLevel level) {
        switch (level) {
            case LEVEL_1:
                return "level1officers";
            case SENIOR:
                return "seniorofficers";
            case SUPERVISOR:
                return "supervisors";
            default:
                return "officers";
        }
    }
    
    @Override
    public void cleanupAllIdentityResources() {
        try {
            log.info("Starting cleanup of all Camunda identity resources");
            
            // Get all users
            List<User> users = identityService.createUserQuery().list();
            
            // Get all groups
            List<Group> groups = identityService.createGroupQuery().list();
            
            // Delete all memberships and users (except admin)
            for (User user : users) {
                String userId = user.getId();
                
                // Skip the admin user
                if ("admin".equals(userId)) {
                    log.info("Skipping admin user during cleanup");
                    continue;
                }
                
                // Delete all memberships for this user
                for (Group group : groups) {
                    try {
                        identityService.deleteMembership(userId, group.getId());
                    } catch (Exception e) {
                        // Ignore errors if membership doesn't exist
                        log.debug("Error deleting membership for user {} in group {}: {}", 
                                userId, group.getId(), e.getMessage());
                    }
                }
                
                // Delete the user
                try {
                    identityService.deleteUser(userId);
                    log.debug("Deleted Camunda user: {}", userId);
                } catch (Exception e) {
                    log.warn("Error deleting Camunda user {}: {}", userId, e.getMessage());
                }
            }
            
            // Delete all groups (except camunda-admin and other system groups)
            for (Group group : groups) {
                String groupId = group.getId();
                
                // Skip system groups
                if ("camunda-admin".equals(groupId) || groupId.startsWith("system_")) {
                    log.info("Skipping system group during cleanup: {}", groupId);
                    continue;
                }
                
                try {
                    identityService.deleteGroup(groupId);
                    log.debug("Deleted Camunda group: {}", groupId);
                } catch (Exception e) {
                    log.warn("Error deleting Camunda group {}: {}", groupId, e.getMessage());
                }
            }
            
            log.info("Completed cleanup of all Camunda identity resources");
        } catch (Exception e) {
            log.error("Error during cleanup of Camunda identity resources", e);
        }
    }
} 