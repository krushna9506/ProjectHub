package com.projecthub.models;

import java.util.ArrayList;
import java.util.List;

public class GitHubItem {
    private int number;
    private String title;
    private String state;
    private String author;
    private String htmlUrl;
    private boolean pullRequest;
    private String updatedAt;
    private List<String> labels;

    public GitHubItem() {
        this.labels = new ArrayList<>();
    }

    public GitHubItem(int number, String title, String state, String author, String htmlUrl,
                      boolean pullRequest, String updatedAt, List<String> labels) {
        this.number = number;
        this.title = title;
        this.state = state;
        this.author = author;
        this.htmlUrl = htmlUrl;
        this.pullRequest = pullRequest;
        this.updatedAt = updatedAt;
        this.labels = labels != null ? labels : new ArrayList<>();
    }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public boolean isPullRequest() { return pullRequest; }
    public void setPullRequest(boolean pullRequest) { this.pullRequest = pullRequest; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getLabels() { return labels != null ? labels : new ArrayList<>(); }
    public void setLabels(List<String> labels) { this.labels = labels != null ? labels : new ArrayList<>(); }
}
