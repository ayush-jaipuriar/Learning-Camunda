/**
 * Dispute Review Page Script
 */
document.addEventListener('DOMContentLoaded', function() {
    // Get case ID from URL
    const urlParams = new URLSearchParams(window.location.search);
    const caseId = urlParams.get('caseId');
    
    if (!caseId) {
        showError('No case ID provided');
        return;
    }
    
    // Initialize PII comparison component
    const piiComparisonComponent = new PIIComparisonComponent('piiComparisonContainer');
    piiComparisonComponent.init(caseId);
    
    // Load dispute details
    loadDisputeDetails(caseId);
});

/**
 * Load dispute details from the server
 */
async function loadDisputeDetails(caseId) {
    try {
        const response = await fetch(`/api/disputes/${caseId}`);
        if (!response.ok) {
            throw new Error('Failed to load dispute details');
        }
        
        const dispute = await response.json();
        displayDisputeDetails(dispute);
    } catch (error) {
        console.error('Error loading dispute details:', error);
        showError(error.message);
    }
}

/**
 * Display dispute details on the page
 */
function displayDisputeDetails(dispute) {
    document.getElementById('caseIdDisplay').textContent = dispute.caseId;
    document.getElementById('disputeTypeDisplay').textContent = dispute.disputeType;
    document.getElementById('statusDisplay').textContent = dispute.status;
    document.getElementById('priorityDisplay').textContent = dispute.priorityLevel;
    document.getElementById('complexityDisplay').textContent = dispute.complexityLevel;
    
    // Format dates
    const submissionDate = new Date(dispute.submissionTimestamp);
    document.getElementById('submissionDateDisplay').textContent = submissionDate.toLocaleString();
    
    if (dispute.assignmentTimestamp) {
        const assignmentDate = new Date(dispute.assignmentTimestamp);
        document.getElementById('assignmentDateDisplay').textContent = assignmentDate.toLocaleString();
    }
    
    // Display user information
    document.getElementById('userIdDisplay').textContent = dispute.userId;
    document.getElementById('userFullNameDisplay').textContent = dispute.submittedUserFullName || 'N/A';
    document.getElementById('userAddressDisplay').textContent = dispute.submittedUserAddress || 'N/A';
    document.getElementById('userPhoneDisplay').textContent = dispute.submittedUserPhoneNumber || 'N/A';
    document.getElementById('userEmailDisplay').textContent = dispute.submittedUserEmailAddress || 'N/A';
    
    // Display description
    document.getElementById('descriptionDisplay').textContent = dispute.description || 'No description provided';
    
    // Display assigned officer if any
    if (dispute.assignedOfficer) {
        document.getElementById('assignedOfficerDisplay').textContent = 
            `${dispute.assignedOfficer.fullName} (${dispute.assignedOfficer.username})`;
    } else {
        document.getElementById('assignedOfficerDisplay').textContent = 'Not assigned';
    }
}

/**
 * Show an error message on the page
 */
function showError(message) {
    const errorContainer = document.getElementById('errorContainer');
    errorContainer.innerHTML = `
        <div class="alert alert-danger">
            <strong>Error:</strong> ${message}
        </div>
    `;
    errorContainer.style.display = 'block';
} 