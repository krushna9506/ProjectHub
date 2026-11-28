package com.projecthub.models;

public class FileReference {
    private String fileId;
    private String projectId;
    private String name;
    private String url;
    private String type; // "DOCUMENT", "IMAGE", "LINK", "CODE", "GITHUB"
    private String snippetContent;
    private String language;
    private String uploadedById;
    private String uploadedByName;
    private String storagePath;
    private long fileSizeBytes;
    private String accessLevel; // "MEMBERS_ONLY", "LEAD_ONLY", "PUBLIC"
    private long timestamp;

    public FileReference() {
        this.type = "LINK";
        this.accessLevel = "MEMBERS_ONLY";
    }

    public FileReference(String fileId, String projectId, String name, String url, 
                         String type, String uploadedByName, long timestamp) {
        this(fileId, projectId, name, url, type, "", "Plain Text", uploadedByName, timestamp);
    }

    public FileReference(String fileId, String projectId, String name, String url, 
                         String type, String snippetContent, String language,
                         String uploadedByName, long timestamp) {
        this.fileId = fileId;
        this.projectId = projectId;
        this.name = name;
        this.url = url;
        this.type = type != null ? type : "LINK";
        this.snippetContent = snippetContent;
        this.language = language != null ? language : "General";
        this.uploadedByName = uploadedByName;
        this.accessLevel = "MEMBERS_ONLY";
        this.timestamp = timestamp;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getType() { return type != null ? type : "LINK"; }
    public void setType(String type) { this.type = type; }

    public String getSnippetContent() { return snippetContent; }
    public void setSnippetContent(String snippetContent) { this.snippetContent = snippetContent; }

    public String getLanguage() { return language != null ? language : "General"; }
    public void setLanguage(String language) { this.language = language; }

    public String getUploadedById() { return uploadedById; }
    public void setUploadedById(String uploadedById) { this.uploadedById = uploadedById; }

    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getAccessLevel() { return accessLevel != null && !accessLevel.isEmpty() ? accessLevel : "MEMBERS_ONLY"; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
