package com.projecthub.models;

public class FileReference {
    private String fileId;
    private String projectId;
    private String name;
    private String url;
    private String type; // "DOCUMENT", "IMAGE", "LINK"
    private String uploadedByName;
    private long timestamp;

    public FileReference() {}

    public FileReference(String fileId, String projectId, String name, String url, 
                         String type, String uploadedByName, long timestamp) {
        this.fileId = fileId;
        this.projectId = projectId;
        this.name = name;
        this.url = url;
        this.type = type;
        this.uploadedByName = uploadedByName;
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

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
