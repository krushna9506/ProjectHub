package com.projecthub.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Project {
    private String projectId;
    private String title;
    private String description;
    private String joinCode;
    private String leadId;
    private String leadName;
    private Map<String, String> members; // userId -> Role ("LEAD", "MEMBER", "FACULTY")
    private int progressPercentage;
    private long createdAt;
    private long deadline;
    private boolean isPublic;
    private String category;   // e.g. "SIH Hackathon", "Capstone", "Research", "Side Project"
    private String lookingFor; // e.g. "Android Dev, UI/UX Designer"
    private String collegeDomain;
    private String githubRepoUrl;
    private String venueLocation;
    private double latitude;
    private double longitude;
    private List<String> customStatuses;
    private String templateType; // "BLANK", "CAPSTONE", "HACKATHON", "RESEARCH"
    private String mentorId;
    private String mentorName;
    private String lastJoinCodeUsed;

    public Project() {
        this.members = new HashMap<>();
        this.customStatuses = defaultStatuses();
        this.templateType = "BLANK";
        this.lastJoinCodeUsed = "";
    }

    public Project(String projectId, String title, String description, String joinCode, 
                   String leadId, String leadName, long deadline) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.joinCode = joinCode;
        this.leadId = leadId;
        this.leadName = leadName;
        this.members = new HashMap<>();
        this.members.put(leadId, "LEAD");
        this.progressPercentage = 0;
        this.createdAt = System.currentTimeMillis();
        this.deadline = deadline;
        this.isPublic = false;
        this.category = "General";
        this.lookingFor = "";
        this.collegeDomain = "";
        this.githubRepoUrl = "";
        this.venueLocation = "";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.customStatuses = defaultStatuses();
        this.templateType = "BLANK";
        this.mentorId = "";
        this.mentorName = "";
    }

    @Exclude
    public static List<String> defaultStatuses() {
        return new ArrayList<>(Arrays.asList("TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"));
    }

    @Exclude
    public List<String> getEffectiveStatuses() {
        if (customStatuses == null || customStatuses.isEmpty()) {
            return defaultStatuses();
        }
        return customStatuses;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }

    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }

    public Map<String, String> getMembers() { return members; }
    public void setMembers(Map<String, String> members) { this.members = members; }

    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getDeadline() { return deadline; }
    public void setDeadline(long deadline) { this.deadline = deadline; }

    @PropertyName("public")
    public boolean isPublic() { return isPublic; }
    @PropertyName("public")
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    @PropertyName("isPublic")
    public boolean getIsPublic() { return isPublic; }
    @PropertyName("isPublic")
    public void setIsPublic(boolean aPublic) { isPublic = aPublic; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLookingFor() { return lookingFor; }
    public void setLookingFor(String lookingFor) { this.lookingFor = lookingFor; }

    public String getCollegeDomain() { return collegeDomain; }
    public void setCollegeDomain(String collegeDomain) { this.collegeDomain = collegeDomain; }

    public String getGithubRepoUrl() { return githubRepoUrl; }
    public void setGithubRepoUrl(String githubRepoUrl) { this.githubRepoUrl = githubRepoUrl; }

    public String getVenueLocation() { return venueLocation; }
    public void setVenueLocation(String venueLocation) { this.venueLocation = venueLocation; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public List<String> getCustomStatuses() { return customStatuses; }
    public void setCustomStatuses(List<String> customStatuses) { this.customStatuses = customStatuses; }

    public String getTemplateType() { return templateType != null ? templateType : "BLANK"; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public String getLastJoinCodeUsed() { return lastJoinCodeUsed; }
    public void setLastJoinCodeUsed(String lastJoinCodeUsed) { this.lastJoinCodeUsed = lastJoinCodeUsed; }
}
