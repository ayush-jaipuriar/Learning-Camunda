package com.example.disputeresolutionsystem.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationHelper implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking and fixing escalated column in disputes table...");
        
        // Check if the column exists
        boolean columnExists = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'disputes' AND column_name = 'escalated')",
            Boolean.class
        );
        
        if (!columnExists) {
            System.out.println("Column 'escalated' does not exist. Adding it with default value...");
            jdbcTemplate.execute("ALTER TABLE disputes ADD COLUMN escalated BOOLEAN DEFAULT false");
        } else {
            System.out.println("Column 'escalated' already exists. Updating NULL values...");
        }
        
        // Update any NULL values
        int updatedRows = jdbcTemplate.update("UPDATE disputes SET escalated = false WHERE escalated IS NULL");
        System.out.println("Updated " + updatedRows + " rows with NULL values.");
        
        // Check if the column is nullable
        boolean isNullable = jdbcTemplate.queryForObject(
            "SELECT is_nullable = 'YES' FROM information_schema.columns WHERE table_name = 'disputes' AND column_name = 'escalated'",
            Boolean.class
        );
        
        if (isNullable) {
            System.out.println("Adding NOT NULL constraint to 'escalated' column...");
            jdbcTemplate.execute("ALTER TABLE disputes ALTER COLUMN escalated SET NOT NULL");
        } else {
            System.out.println("Column 'escalated' already has NOT NULL constraint.");
        }
        
        System.out.println("Database migration completed successfully.");
    }
} 