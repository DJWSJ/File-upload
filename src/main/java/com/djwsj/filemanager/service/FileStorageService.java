package com.djwsj.filemanager.service;

import com.djwsj.filemanager.entity.FileInfo;
import com.djwsj.filemanager.enums.FileCategory;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    // 设置为最大限制 2GB
    @Value("${file.max-size:2147483647}") // 2GB
    private long maxFileSize;

    @Value("${file.allowed-extensions:jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip,rar,mp4,avi,mp3,wav}")
    private String allowedExtensions;

    // 内存中存储文件信息（实际项目中建议使用数据库）
    private final Map<String, FileInfo> fileInfoCache = new ConcurrentHashMap<>();

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    /**
     * 存储文件（兼容原有接口）
     */
    public FileInfo storeFile(MultipartFile file) {
        return storeFile(file, null, "system");
    }

    /**
     * 存储文件（新接口，支持分类和上传用户）
     */
    public FileInfo storeFile(MultipartFile file, FileCategory category, String uploadUser) {
        // 验证文件
        validateFile(file);

        // 清理文件名
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = getFileExtension(originalFileName);
        String fileName = generateUniqueFileName(originalFileName);

        try {
            // 检查文件名是否包含非法字符
            if (fileName.contains("..")) {
                throw new RuntimeException("文件名包含非法路径序列: " + fileName);
            }

            // 复制文件到目标位置
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 自动确定文件分类（如果未指定）
            if (category == null) {
                category = determineFileCategory(fileExtension);
            }

            // 使用默认构造函数和setter创建FileInfo
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFilename(fileName);
            fileInfo.setOriginalFilename(originalFileName);
            fileInfo.setSize(file.getSize());
            fileInfo.setFileType(file.getContentType());
            fileInfo.setUploadTime(LocalDateTime.now());
            fileInfo.setCategory(category);
            fileInfo.setExtension(fileExtension);
            fileInfo.setUploadUser(uploadUser);
            fileInfo.setFilePath(targetLocation.toString());
            fileInfo.setDownloadUrl("/download/" + fileName);

            // 缓存文件信息
            fileInfoCache.put(fileName, fileInfo);

            return fileInfo;

        } catch (IOException ex) {
            throw new RuntimeException("无法存储文件 " + fileName + "，请重试!", ex);
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 验证文件大小 - 使用配置的最大值
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("文件大小不能超过 " + formatFileSize(maxFileSize));
        }

        // 验证文件类型
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new RuntimeException("文件名不能为空");
        }

        String fileExtension = getFileExtension(originalFileName);
        if (!isAllowedExtension(fileExtension)) {
            throw new RuntimeException("不支持的文件类型: " + fileExtension);
        }
    }

    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long size) {
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
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFileName) {
        String fileExtension = getFileExtension(originalFileName);
        String baseName = originalFileName;
        if (fileExtension != null && !fileExtension.isEmpty()) {
            baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8);

        if (fileExtension != null && !fileExtension.isEmpty()) {
            return timestamp + "_" + random + "_" + baseName + "." + fileExtension;
        } else {
            return timestamp + "_" + random + "_" + baseName;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return null;
    }

    /**
     * 检查文件扩展名是否允许
     */
    private boolean isAllowedExtension(String fileExtension) {
        if (fileExtension == null) return false;

        String[] allowedExts = allowedExtensions.split(",");
        for (String ext : allowedExts) {
            if (ext.trim().equalsIgnoreCase(fileExtension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据文件扩展名确定文件分类
     */
    private FileCategory determineFileCategory(String fileExtension) {
        if (fileExtension == null) return FileCategory.OTHER;

        switch (fileExtension.toLowerCase()) {
            case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp":
                return FileCategory.IMAGE;
            case "pdf": case "doc": case "docx": case "txt": case "ppt": case "pptx": case "xls": case "xlsx":
                return FileCategory.DOCUMENT;
            case "mp4": case "avi": case "mov": case "wmv": case "flv": case "mkv":
                return FileCategory.VIDEO;
            case "mp3": case "wav": case "aac": case "flac":
                return FileCategory.AUDIO;
            case "zip": case "rar": case "7z": case "tar": case "gz":
                return FileCategory.ARCHIVE;
            case "exe": case "jar": case "msi": case "bat": case "cmd":
                return FileCategory.EXECUTABLE;
            default:
                return FileCategory.OTHER;
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("文件未找到或不可读: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("文件路径格式错误: " + fileName, ex);
        } catch (IOException ex) {
            throw new RuntimeException("无法访问文件: " + fileName, ex);
        }
    }

    /**
     * 获取所有文件（优先从缓存获取，缓存不存在则从文件系统读取）
     */
    public List<FileInfo> getAllFiles() {
        try {
            if (!Files.exists(this.fileStorageLocation)) {
                return new ArrayList<>();
            }

            List<FileInfo> files = Files.list(this.fileStorageLocation)
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            String fileName = path.getFileName().toString();

                            // 优先从缓存获取
                            FileInfo cachedInfo = fileInfoCache.get(fileName);
                            if (cachedInfo != null) {
                                return cachedInfo;
                            }

                            // 缓存不存在，从文件系统读取
                            String originalFileName = extractOriginalFileName(fileName);
                            String contentType = Files.probeContentType(path);
                            if (contentType == null) {
                                contentType = "application/octet-stream";
                            }

                            // 使用默认构造函数和setter创建FileInfo
                            FileInfo fileInfo = new FileInfo();
                            fileInfo.setFilename(fileName);
                            fileInfo.setOriginalFilename(originalFileName);
                            fileInfo.setSize(Files.size(path));
                            fileInfo.setFileType(contentType);
                            fileInfo.setUploadTime(
                                    Files.getLastModifiedTime(path).toInstant()
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDateTime()
                            );

                            // 设置额外信息
                            String fileExtension = getFileExtension(originalFileName);
                            fileInfo.setCategory(determineFileCategory(fileExtension));
                            fileInfo.setExtension(fileExtension);
                            fileInfo.setFilePath(path.toString());
                            fileInfo.setDownloadUrl("/download/" + fileName);

                            // 加入缓存
                            fileInfoCache.put(fileName, fileInfo);

                            return fileInfo;
                        } catch (IOException e) {
                            System.err.println("无法读取文件信息: " + path.getFileName() + " - " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 按上传时间倒序排列
            files.sort((f1, f2) -> f2.getUploadTime().compareTo(f1.getUploadTime()));
            return files;

        } catch (IOException e) {
            System.err.println("无法列出文件: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 从存储的文件名中提取原始文件名
     */
    private String extractOriginalFileName(String storedFileName) {
        if (storedFileName == null) return "";

        // 格式：timestamp_random_originalFilename.extension
        String[] parts = storedFileName.split("_", 3);
        if (parts.length >= 3) {
            return parts[2];
        } else {
            return storedFileName;
        }
    }

    /**
     * 获取特定分类的文件
     */
    public List<FileInfo> getFilesByCategory(FileCategory category) {
        return getAllFiles().stream()
                .filter(file -> file.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * 按分类分组获取文件
     */
    public Map<FileCategory, List<FileInfo>> getFilesByCategory() {
        return getAllFiles().stream()
                .collect(Collectors.groupingBy(FileInfo::getCategory));
    }

    /**
     * 获取分类统计信息
     */
    public Map<FileCategory, Long> getCategoryStatistics() {
        try {
            List<FileInfo> allFiles = getAllFiles();
            return allFiles.stream()
                    .collect(Collectors.groupingBy(FileInfo::getCategory, Collectors.counting()));
        } catch (Exception e) {
            System.err.println("无法获取分类统计: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            // 安全检查：确保文件在上传目录内
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("不允许删除该文件路径");
            }

            boolean deleted = Files.deleteIfExists(filePath);

            // 从缓存中移除
            if (deleted) {
                fileInfoCache.remove(fileName);
            }

            System.out.println("删除文件: " + fileName + ", 结果: " + deleted);
            return deleted;

        } catch (IOException e) {
            System.err.println("删除文件时出错: " + fileName + " - " + e.getMessage());
            throw new RuntimeException("无法删除文件: " + fileName, e);
        } catch (SecurityException e) {
            System.err.println("安全异常: " + e.getMessage());
            throw new RuntimeException("文件删除被拒绝: " + e.getMessage());
        }
    }

    /**
     * 获取文件信息
     */
    public FileInfo getFileInfo(String fileName) {
        // 优先从缓存获取
        FileInfo cachedInfo = fileInfoCache.get(fileName);
        if (cachedInfo != null) {
            return cachedInfo;
        }

        // 缓存不存在，从文件系统获取
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!Files.exists(filePath)) {
                return null;
            }

            String originalFileName = extractOriginalFileName(fileName);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 使用默认构造函数和setter创建FileInfo
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFilename(fileName);
            fileInfo.setOriginalFilename(originalFileName);
            fileInfo.setSize(Files.size(filePath));
            fileInfo.setFileType(contentType);
            fileInfo.setUploadTime(
                    Files.getLastModifiedTime(filePath).toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
            );

            // 设置额外信息
            String fileExtension = getFileExtension(originalFileName);
            fileInfo.setCategory(determineFileCategory(fileExtension));
            fileInfo.setExtension(fileExtension);
            fileInfo.setFilePath(filePath.toString());
            fileInfo.setDownloadUrl("/download/" + fileName);

            // 加入缓存
            fileInfoCache.put(fileName, fileInfo);

            return fileInfo;

        } catch (IOException e) {
            System.err.println("获取文件信息失败: " + fileName + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取存储目录信息
     */
    public Map<String, Object> getStorageInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            if (Files.exists(this.fileStorageLocation)) {
                long totalSize = Files.list(this.fileStorageLocation)
                        .filter(Files::isRegularFile)
                        .mapToLong(path -> {
                            try {
                                return Files.size(path);
                            } catch (IOException e) {
                                return 0L;
                            }
                        })
                        .sum();

                long fileCount = Files.list(this.fileStorageLocation)
                        .filter(Files::isRegularFile)
                        .count();

                info.put("storagePath", this.fileStorageLocation.toString());
                info.put("totalSize", totalSize);
                info.put("totalFiles", fileCount);
                info.put("freeSpace", this.fileStorageLocation.toFile().getFreeSpace());
                info.put("usableSpace", this.fileStorageLocation.toFile().getUsableSpace());
                info.put("totalSpace", this.fileStorageLocation.toFile().getTotalSpace());
            }
        } catch (IOException e) {
            System.err.println("获取存储信息失败: " + e.getMessage());
        }
        return info;
    }

    /**
     * 清空缓存（用于测试或手动刷新）
     */
    public void clearCache() {
        fileInfoCache.clear();
    }

    /**
     * 获取缓存中的文件数量
     */
    public int getCacheSize() {
        return fileInfoCache.size();
    }
}