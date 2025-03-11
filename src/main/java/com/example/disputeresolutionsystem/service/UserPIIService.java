package com.example.disputeresolutionsystem.service;

import com.example.disputeresolutionsystem.model.CaseOfficer;
import com.example.disputeresolutionsystem.model.UserPII;
import com.example.disputeresolutionsystem.repository.UserPIIRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPIIService {

    private final UserPIIRepository userPIIRepository;
    private final Random random = new Random();
    
    private static final List<String> SAMPLE_ADDRESSES = Arrays.asList(
        "123 Main St, Anytown, CA 92345",
        "456 Oak Ave, Springfield, IL 62701",
        "789 Pine Rd, Liberty, NY 10001",
        "101 Maple Dr, Westview, TX 77001",
        "202 Cedar Ln, Lakeview, FL 33101"
    );
    
    private static final List<String> SAMPLE_PHONE_NUMBERS = Arrays.asList(
        "555-123-4567",
        "555-234-5678",
        "555-345-6789",
        "555-456-7890",
        "555-567-8901"
    );
    
    /**
     * Create dummy PII data for a case officer
     */
    @Transactional
    public void createDummyPIIForOfficer(CaseOfficer officer) {
        log.info("Creating dummy PII data for officer: {}", officer.getUsername());
        
        // Create a few dummy user profiles associated with this officer
        int numProfiles = 3 + random.nextInt(3); // 3-5 profiles
        
        for (int i = 0; i < numProfiles; i++) {
            UserPII userPII = new UserPII();
            
            // Base username on officer's username + number
            String username = officer.getUsername() + "_user" + (i + 1);
            userPII.setUsername(username);
            
            // Generate fullName based on username
            String fullName = generateFullName(username);
            userPII.setFullName(fullName);
            
            // Random address
            userPII.setAddress(SAMPLE_ADDRESSES.get(random.nextInt(SAMPLE_ADDRESSES.size())));
            
            // Random phone number
            userPII.setPhoneNumber(SAMPLE_PHONE_NUMBERS.get(random.nextInt(SAMPLE_PHONE_NUMBERS.size())));
            
            // Random SSN (format: XXX-XX-XXXX)
            userPII.setSsn(generateRandomSSN());
            
            // Random date of birth (adults between 25-65 years old)
            userPII.setDateOfBirth(generateRandomDOB(25, 65));
            
            // Email based on username
            userPII.setEmailAddress(username + "@example.com");
            
            // Associate with officer
            userPII.setCaseOfficer(officer);
            
            userPIIRepository.save(userPII);
            log.info("Created dummy PII data for user: {}", username);
        }
    }
    
    /**
     * Get PII data for a specific username
     */
    public Optional<UserPII> getPIIByUsername(String username) {
        return userPIIRepository.findByUsername(username);
    }
    
    /**
     * Delete all PII data associated with a case officer
     */
    @Transactional
    public void deletePIIForOfficer(Long officerId) {
        log.info("Deleting all PII data for officer ID: {}", officerId);
        userPIIRepository.deleteAllByCaseOfficerId(officerId);
    }
    
    /**
     * Helper method to generate a random SSN
     */
    private String generateRandomSSN() {
        return String.format("%03d-%02d-%04d", 
            100 + random.nextInt(900),
            10 + random.nextInt(90),
            1000 + random.nextInt(9000));
    }
    
    /**
     * Helper method to generate a random date of birth
     */
    private String generateRandomDOB(int minAge, int maxAge) {
        int year = LocalDate.now().getYear() - (minAge + random.nextInt(maxAge - minAge + 1));
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28); // Simplified - avoids issues with month lengths
        
        return LocalDate.of(year, month, day)
            .format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }
    
    /**
     * Helper method to generate a full name from username
     */
    private String generateFullName(String username) {
        // Convert username like "johndoe_user1" to "John Doe"
        String baseName = username.split("_")[0]; // Get the part before "_user1" 
        
        if (baseName.length() <= 2) {
            return "User " + username; // Fallback for very short usernames
        }
        
        // Try to find two parts in the username for first/last name
        String firstName = "";
        String lastName = "";
        
        // Simple algorithm - find likely split between first and last name
        for (int i = 1; i < baseName.length() - 1; i++) {
            if (Character.isUpperCase(baseName.charAt(i))) {
                firstName = baseName.substring(0, i);
                lastName = baseName.substring(i);
                break;
            }
        }
        
        // If we couldn't find a clear split, just use half the string for each
        if (firstName.isEmpty()) {
            int mid = baseName.length() / 2;
            firstName = baseName.substring(0, mid);
            lastName = baseName.substring(mid);
        }
        
        // Capitalize first letter of each name
        firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);
        lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1);
        
        return firstName + " " + lastName;
    }

    /**
     * Save a UserPII object
     */
    @Transactional
    public UserPII saveUserPII(UserPII userPII) {
        log.info("Saving UserPII for username: {}", userPII.getUsername());
        return userPIIRepository.save(userPII);
    }
} 