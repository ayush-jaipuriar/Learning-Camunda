# Dispute Resolution System with PII Validation

This application demonstrates a Dispute Resolution System with PII (Personally Identifiable Information) validation integrated with Camunda BPM.

## Features

- Dispute submission with PII information
- Document upload and management
- Automatic PII comparison against database records
- Case assignment to officers based on complexity
- Complete dispute review and resolution workflow

## Getting Started

### Prerequisites

- Java 17 or later
- PostgreSQL database
- Camunda BPM 7.18 or later (included in dependencies)

### Setup

1. Clone this repository
2. Configure database settings in `application.properties`
3. Run the application using:
   ```
   ./gradlew bootRun
   ```
   or use the `rebuild_and_restart.bat` script

## Testing the PII Validation Workflow

### Using the API (Postman)

1. **Submit a dispute with PII information**:
   ```
   POST http://localhost:8080/api/disputes/submit
   ```
   With form data:
   - `dispute`: JSON with user details
   - `documents`: Optional file attachment

   Example dispute JSON:
   ```json
   {
     "userId": "john_user1",
     "disputeType": "address correction",
     "creditReportId": "CR-12345",
     "userFullName": "John Doe",
     "userAddress": "123 Main St, Anytown, CA 92345",
     "userPhoneNumber": "555-123-4567",
     "userEmailAddress": "john_user1@example.com",
     "description": "My address is incorrect on my credit report."
   }
   ```

2. **Access Camunda Tasklist**:
   - Navigate to `http://localhost:8080/camunda/app/tasklist`
   - Login with admin credentials (default: admin/admin)

3. **View and Process Tasks**:
   - Find tasks in the task list
   - Open a task to see the PII comparison information directly in the form
   - Update the PII validation status
   - Complete the review task

### Important PII Validation Fields in Camunda Form

The following fields will be automatically populated in the task form:

- **PII Validation Status**: Current status (PENDING, MATCH, PARTIAL_MATCH, MISMATCH, NOT_FOUND)
- **Submitted vs Database Fields**: 
  - Full Name (submitted and database values, plus match indicator)
  - Address (submitted and database values, plus match indicator)
  - Phone Number (submitted and database values, plus match indicator)
  - Email Address (submitted and database values, plus match indicator)
- **PII Notes**: Field for entering notes about the PII validation

## Troubleshooting

- **Process not starting**: Check logs for errors when submitting disputes
- **Missing PII data**: Ensure the database contains matching user records
- **Task assignment issues**: Verify officer records exist in the database
- **Camunda errors**: Check the Camunda admin console at `http://localhost:8080/camunda/app/admin`

## Additional Information

- All dispute processing is now handled within the Camunda Tasklist interface
- No separate UI pages are needed, as all functionality is available in Camunda forms
- PII comparison is performed automatically when a review task is created

## License

This project is licensed under the MIT License - see the LICENSE file for details.