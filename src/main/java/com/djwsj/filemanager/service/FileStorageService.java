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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    public FileInfo storeFile(MultipartFile file) {
        // 清理文件名
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = System.currentTimeMillis() + "_" + originalFileName;

        try {
            // 检查文件名是否包含非法字符
            if (fileName.contains("..")) {
                throw new RuntimeException("文件名包含非法路径序列: " + fileName);
            }

            // 复制文件到目标位置
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 创建文件信息
            FileInfo fileInfo = new FileInfo(
                    fileName,
                    originalFileName,
                    file.getSize(),
                    file.getContentType(),
                    LocalDateTime.now()
            );

            return fileInfo;

        } catch (IOException ex) {
            throw new RuntimeException("无法存储文件 " + fileName + "，请重试!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            UrlResource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("文件未找到: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("文件未找到: " + fileName, ex);
        } catch (IOException ex) {
            throw new RuntimeException("无法访问文件: " + fileName, ex);
        }
    }

    public List<FileInfo> getAllFiles() {
        try {
            if (!Files.exists(this.fileStorageLocation)) {
                return List.of();
            }

            return Files.list(this.fileStorageLocation)
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            String originalFileName = fileName.contains("_") ?
                                    fileName.substring(fileName.indexOf("_") + 1) : fileName;

                            String contentType = Files.probeContentType(path);
                            if (contentType == null) {
                                contentType = "application/octet-stream";
                            }

                            FileInfo fileInfo = new FileInfo(
                                    fileName,
                                    originalFileName,
                                    Files.size(path),
                                    contentType,
                                    Files.getLastModifiedTime(path).toInstant()
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDateTime()
                            );

                            return fileInfo;
                        } catch (IOException e) {
                            // 记录错误但继续处理其他文件
                            System.err.println("无法读取文件信息: " + path.getFileName() + " - " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull) // 过滤掉null值
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("无法列出文件: " + e.getMessage());
            return List.of();
        }
    }

    // 按分类获取文件
    public Map<FileCategory, List<FileInfo>> getFilesByCategory() throws IOException {
        List<FileInfo> allFiles = getAllFiles();
        return allFiles.stream()
                .collect(Collectors.groupingBy(FileInfo::getCategory));
    }

    // 获取特定分类的文件
    public List<FileInfo> getFilesByCategory(FileCategory category) throws IOException {
        return getAllFiles().stream()
                .filter(file -> file.getCategory() == category)
                .collect(Collectors.toList());
    }

    // 获取分类统计信息
    public Map<FileCategory, Long> getCategoryStatistics() {
        try {
            List<FileInfo> allFiles = getAllFiles();
            return allFiles.stream()
                    .collect(Collectors.groupingBy(FileInfo::getCategory, Collectors.counting()));
        } catch (Exception e) {
            System.err.println("无法获取分类统计: " + e.getMessage());
            return Map.of();
        }
    }

    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            // 安全检查：确保文件在上传目录内
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("不允许删除该文件路径");
            }

            boolean deleted = Files.deleteIfExists(filePath);
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
}