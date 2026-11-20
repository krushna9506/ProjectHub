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

    public User() {
        this.joinedProjectIds = new ArrayList<>();
    }

    public User(String userId, String name, String email, String department, String rollNumber) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.rollNumber = rollNumber;
        this.joinedProjectIds = new ArrayList<>();
    }

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

    public List<String> getJoinedProjectIds() { return joinedProjectIds; }
    public void setJoinedProjectIds(List<String> joinedProjectIds) { this.joinedProjectIds = joinedProjectIds; }
}
