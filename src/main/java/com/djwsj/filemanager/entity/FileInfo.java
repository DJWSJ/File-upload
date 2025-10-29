// src/main/java/com/djwsj/filemanager/entity/FileInfo.java
package com.djwsj.filemanager.entity;

import com.djwsj.filemanager.enums.FileCategory;

import java.time.LocalDateTime;

public class FileInfo {
    private String id; // 文件唯一标识
    private String filename; // 存储的文件名
    private String originalFilename; // 原始文件名
    private long size; // 文件大小（字节）
    private String fileType; // MIME类型
    private LocalDateTime uploadTime; // 上传时间
    private String downloadUrl; // 下载URL
    private String filePath; // 文件存储路径
    private FileCategory category; // 文件分类
    private String extension; // 文件扩展名
    private String uploadUser; // 上传用户

    // 默认构造方法
    public FileInfo() {
        this.uploadTime = LocalDateTime.now();
    }

    // 完整构造方法
    public FileInfo(String id, String filename, String originalFilename, long size,
                    String fileType, String filePath, String uploadUser) {
        this.id = id;
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.size = size;
        this.fileType = fileType;
        this.filePath = filePath;
        this.uploadUser = uploadUser;
        this.uploadTime = LocalDateTime.now();
        this.extension = extractExtension(originalFilename);
        this.category = FileCategory.fromFilename(originalFilename);
    }

    // 简化构造方法
    public FileInfo(String filename, String originalFilename, long size,
                    String fileType, String filePath, String uploadUser) {
        this(null, filename, originalFilename, size, fileType, filePath, uploadUser);
    }

    /**
     * 从文件名提取扩展名
     */
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

    /**
     * 获取可读的文件大小（自动转换单位）
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 检查文件是否是图片类型
     */
    public boolean isImage() {
        return category == FileCategory.IMAGE;
    }

    /**
     * 检查文件是否是文档类型
     */
    public boolean isDocument() {
        return category == FileCategory.DOCUMENT;
    }

    /**
     * 检查文件是否是视频类型
     */
    public boolean isVideo() {
        return category == FileCategory.VIDEO;
    }

    /**
     * 检查文件是否是音频类型
     */
    public boolean isAudio() {
        return category == FileCategory.AUDIO;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

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

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public FileCategory getCategory() { return category; }
    public void setCategory(FileCategory category) { this.category = category; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getUploadUser() { return uploadUser; }
    public void setUploadUser(String uploadUser) { this.uploadUser = uploadUser; }

    @Override
    public String toString() {
        return "FileInfo{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", size=" + size +
                ", fileType='" + fileType + '\'' +
                ", uploadTime=" + uploadTime +
                ", category=" + category +
                ", extension='" + extension + '\'' +
                ", uploadUser='" + uploadUser + '\'' +
                '}';
    }
}