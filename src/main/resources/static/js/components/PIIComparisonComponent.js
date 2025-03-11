/**
 * Component for displaying and comparing PII data
 */
class PIIComparisonComponent {
    constructor(containerId) {
        this.containerId = containerId;
        this.container = document.getElementById(containerId);
        this.caseId = null;
        this.comparisonData = null;
    }

    /**
     * Initialize the component with a case ID
     */
    init(caseId) {
        this.caseId = caseId;
        this.loadComparisonData();
    }

    /**
     * Load PII comparison data from the server
     */
    async loadComparisonData() {
        try {
            const response = await fetch(`/api/disputes/${this.caseId}/pii-comparison`);
            if (!response.ok) {
                throw new Error('Failed to load PII comparison data');
            }
            
            this.comparisonData = await response.json();
            this.render();
        } catch (error) {
            console.error('Error loading PII comparison data:', error);
            this.renderError(error.message);
        }
    }

    /**
     * Render the comparison component
     */
    render() {
        if (!this.comparisonData) {
            this.renderError('No comparison data available');
            return;
        }

        const data = this.comparisonData;
        const matchPercentage = data.matchPercentage;
        
        let statusClass = 'badge-warning';
        if (data.validationStatus === 'MATCH') {
            statusClass = 'badge-success';
        } else if (data.validationStatus === 'MISMATCH') {
            statusClass = 'badge-danger';
        } else if (data.validationStatus === 'NOT_FOUND') {
            statusClass = 'badge-dark';
        }

        const html = `
            <div class="card mb-4">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">PII Validation</h5>
                    <span class="badge ${statusClass}">${data.validationStatus}</span>
                </div>
                <div class="card-body">
                    <div class="alert alert-info">
                        <strong>Match Percentage:</strong> ${matchPercentage}%
                    </div>
                    
                    <table class="table table-bordered">
                        <thead>
                            <tr>
                                <th>Field</th>
                                <th>Submitted Value</th>
                                <th>Database Value</th>
                                <th>Match</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr class="${data.fullNameMatch ? 'table-success' : 'table-danger'}">
                                <td>Full Name</td>
                                <td>${data.submittedUserFullName || 'N/A'}</td>
                                <td>${data.databaseUserFullName || 'N/A'}</td>
                                <td>
                                    ${data.fullNameMatch 
                                        ? '<i class="fas fa-check-circle text-success"></i>' 
                                        : '<i class="fas fa-times-circle text-danger"></i>'}
                                </td>
                            </tr>
                            <tr class="${data.addressMatch ? 'table-success' : 'table-danger'}">
                                <td>Address</td>
                                <td>${data.submittedUserAddress || 'N/A'}</td>
                                <td>${data.databaseUserAddress || 'N/A'}</td>
                                <td>
                                    ${data.addressMatch 
                                        ? '<i class="fas fa-check-circle text-success"></i>' 
                                        : '<i class="fas fa-times-circle text-danger"></i>'}
                                </td>
                            </tr>
                            <tr class="${data.phoneNumberMatch ? 'table-success' : 'table-danger'}">
                                <td>Phone Number</td>
                                <td>${data.submittedUserPhoneNumber || 'N/A'}</td>
                                <td>${data.databaseUserPhoneNumber || 'N/A'}</td>
                                <td>
                                    ${data.phoneNumberMatch 
                                        ? '<i class="fas fa-check-circle text-success"></i>' 
                                        : '<i class="fas fa-times-circle text-danger"></i>'}
                                </td>
                            </tr>
                            <tr class="${data.emailMatch ? 'table-success' : 'table-danger'}">
                                <td>Email Address</td>
                                <td>${data.submittedUserEmailAddress || 'N/A'}</td>
                                <td>${data.databaseUserEmailAddress || 'N/A'}</td>
                                <td>
                                    ${data.emailMatch 
                                        ? '<i class="fas fa-check-circle text-success"></i>' 
                                        : '<i class="fas fa-times-circle text-danger"></i>'}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                    
                    <div class="form-group mt-4">
                        <label for="piiValidationStatus">Update Validation Status:</label>
                        <select class="form-control" id="piiValidationStatus">
                            <option value="PENDING" ${data.validationStatus === 'PENDING' ? 'selected' : ''}>Pending</option>
                            <option value="MATCH" ${data.validationStatus === 'MATCH' ? 'selected' : ''}>Match</option>
                            <option value="PARTIAL_MATCH" ${data.validationStatus === 'PARTIAL_MATCH' ? 'selected' : ''}>Partial Match</option>
                            <option value="MISMATCH" ${data.validationStatus === 'MISMATCH' ? 'selected' : ''}>Mismatch</option>
                            <option value="NOT_FOUND" ${data.validationStatus === 'NOT_FOUND' ? 'selected' : ''}>Not Found</option>
                        </select>
                    </div>
                    
                    <div class="form-group">
                        <label for="piiValidationNotes">Notes:</label>
                        <textarea class="form-control" id="piiValidationNotes" rows="3">${data.validationNotes || ''}</textarea>
                    </div>
                    
                    <button id="updatePiiValidationBtn" class="btn btn-primary">Update Validation</button>
                </div>
            </div>
        `;

        this.container.innerHTML = html;
        this.attachEventListeners();
    }

    /**
     * Render an error message
     */
    renderError(message) {
        this.container.innerHTML = `
            <div class="alert alert-danger">
                <strong>Error:</strong> ${message}
            </div>
        `;
    }

    /**
     * Attach event listeners to the component
     */
    attachEventListeners() {
        const updateButton = document.getElementById('updatePiiValidationBtn');
        if (updateButton) {
            updateButton.addEventListener('click', () => this.updateValidationStatus());
        }
    }

    /**
     * Update the validation status
     */
    async updateValidationStatus() {
        const status = document.getElementById('piiValidationStatus').value;
        const notes = document.getElementById('piiValidationNotes').value;
        
        try {
            const response = await fetch(`/api/disputes/${this.caseId}/pii-validation?status=${status}&notes=${encodeURIComponent(notes)}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (!response.ok) {
                throw new Error('Failed to update validation status');
            }
            
            // Reload the data to reflect changes
            this.loadComparisonData();
            
            // Show success message
            alert('Validation status updated successfully');
        } catch (error) {
            console.error('Error updating validation status:', error);
            alert(`Error: ${error.message}`);
        }
    }
} 