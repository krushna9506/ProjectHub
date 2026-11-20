package com.projecthub.models;

public class JoinRequest {
    private String requestId;
    private String projectId;
    private String projectTitle;
    private String leadId;
    private String applicantId;
    private String applicantName;
    private String applicantEmail;
    private String message;
    private String status; // "PENDING", "ACCEPTED", "REJECTED"
    private long timestamp;

    public JoinRequest() {}

    public JoinRequest(String requestId, String projectId, String projectTitle, String leadId,
                       String applicantId, String applicantName, String applicantEmail, 
                       String message, String status, long timestamp) {
        this.requestId = requestId;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.leadId = leadId;
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }

    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
