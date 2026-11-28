package com.projecthub.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String name;
    private String email;
    private String department;
    private String rollNumber;
    private String profileImageUrl;
    private List<String> joinedProjectIds;
    private String fcmToken;
    private String role; // "STUDENT", "FACULTY_MENTOR"
    private String collegeDomain;
    private boolean campusVerified;
    private List<String> skills;
    private String bio;
    private String githubUsername;
    private int graduationYear;

    public User() {
        this.joinedProjectIds = new ArrayList<>();
        this.skills = new ArrayList<>();
        this.role = "STUDENT";
        this.campusVerified = false;
    }

    public User(String userId, String name, String email, String department, String rollNumber) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.rollNumber = rollNumber;
        this.joinedProjectIds = new ArrayList<>();
        this.skills = new ArrayList<>();
        this.role = "STUDENT";
        this.campusVerified = false;
        this.bio = "";
        this.githubUsername = "";
        this.graduationYear = 0;
    }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public List<String> getJoinedProjectIds() { return joinedProjectIds != null ? joinedProjectIds : new ArrayList<>(); }
    public void setJoinedProjectIds(List<String> joinedProjectIds) { this.joinedProjectIds = joinedProjectIds != null ? joinedProjectIds : new ArrayList<>(); }

    public String getRole() { return role != null && !role.isEmpty() ? role : "STUDENT"; }
    public void setRole(String role) { this.role = role; }

    public String getCollegeDomain() { return collegeDomain; }
    public void setCollegeDomain(String collegeDomain) { this.collegeDomain = collegeDomain; }

    public boolean isCampusVerified() { return campusVerified; }
    public void setCampusVerified(boolean campusVerified) { this.campusVerified = campusVerified; }

    public List<String> getSkills() { return skills != null ? skills : new ArrayList<>(); }
    public void setSkills(List<String> skills) { this.skills = skills != null ? skills : new ArrayList<>(); }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }

    public int getGraduationYear() { return graduationYear; }
    public void setGraduationYear(int graduationYear) { this.graduationYear = graduationYear; }
}
