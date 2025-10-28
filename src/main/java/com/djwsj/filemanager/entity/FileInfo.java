// src/main/java/com/djwsj/filemanager/entity/FileInfo.java
package com.djwsj.filemanager.entity;

import com.djwsj.filemanager.enums.FileCategory;

import java.time.LocalDateTime;

public class FileInfo {
    private String filename;
    private String originalFilename;
    private long size;
    private String fileType;
    private LocalDateTime uploadTime;
    private String downloadUrl;
    private FileCategory category;
    private String extension;

    public FileInfo() {}

    public FileInfo(String filename, String originalFilename, long size,
                    String fileType, LocalDateTime uploadTime) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.size = size;
        this.fileType = fileType;
        this.uploadTime = uploadTime;
        this.extension = extractExtension(originalFilename);
        this.category = FileCategory.fromFilename(originalFilename);
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    // Getters and Setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
        this.extension = extractExtension(originalFilename);
        this.category = FileCategory.fromFilename(originalFilename);
    }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public FileCategory getCategory() { return category; }
    public void setCategory(FileCategory category) { this.category = category; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
}