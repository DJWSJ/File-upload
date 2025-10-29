package com.djwsj.filemanager.controller;

import com.djwsj.filemanager.entity.FileInfo;
import com.djwsj.filemanager.enums.FileCategory;
import com.djwsj.filemanager.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/api")
public class FileController {

    private final FileStorageService fileStorageService;

    // 最大文件大小 2GB
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // ==================== 根路径重定向 ====================

    @GetMapping("/")
    public String apiRoot() {
        return "redirect:/api/file-manager";
    }

    // ==================== Web页面接口 ====================

    @GetMapping("/file-manager")
    public String listFiles(Model model,
                            @RequestParam(required = false) String category) {
        try {
            List<FileInfo> files;

            if (category != null && !category.isEmpty()) {
                // 按分类筛选
                try {
                    FileCategory fileCategory = FileCategory.valueOf(category.toUpperCase());
                    files = fileStorageService.getFilesByCategory(fileCategory);
                } catch (IllegalArgumentException e) {
                    // 如果分类不存在，返回所有文件
                    files = fileStorageService.getAllFiles();
                }
            } else {
                // 所有文件
                files = fileStorageService.getAllFiles();
            }

            // 设置下载URL
            files.forEach(file ->
                    file.setDownloadUrl("/api/download/" + file.getFilename()));

            // 获取分类统计
            Map<FileCategory, Long> categoryStats = fileStorageService.getCategoryStatistics();

            model.addAttribute("files", files != null ? files : new ArrayList<>());
            model.addAttribute("categories", FileCategory.values());
            model.addAttribute("categoryStats", categoryStats != null ? categoryStats : new HashMap<>());
            model.addAttribute("currentCategory", category);

        } catch (Exception e) {
            model.addAttribute("error", "无法读取文件列表: " + e.getMessage());
            model.addAttribute("files", new ArrayList<>());
            model.addAttribute("categoryStats", new HashMap<>());
        }
        return "file-list";
    }

    @GetMapping("/category/{category}")
    public String listFilesByCategory(@PathVariable String category, Model model) {
        return listFiles(model, category);
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        return handleFileUpload(file, redirectAttributes, "redirect:/api/file-manager");
    }

    @PostMapping("/delete/{fileName}")
    public String deleteFile(@PathVariable String fileName,
                             RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = fileStorageService.deleteFile(fileName);
            if (deleted) {
                redirectAttributes.addFlashAttribute("message", "文件删除成功");
            } else {
                redirectAttributes.addFlashAttribute("message", "文件未找到");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "文件删除失败: " + e.getMessage());
        }
        return "redirect:/api/file-manager";
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName,
                                                 HttpServletRequest request) {
        return handleFileDownload(fileName, request);
    }

    // ==================== 软件端REST API接口 ====================

    /**
     * 软件端 - 单文件上传接口
     */
    @PostMapping(value = "/v1/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFileApi(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadUser", defaultValue = "system") String uploadUser,
            @RequestParam(value = "category", required = false) String category) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证文件
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("code", 400);
                response.put("message", "文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 文件大小验证 - 改为最大2GB
            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("success", false);
                response.put("code", 400);
                response.put("message", "文件大小不能超过 2GB");
                return ResponseEntity.badRequest().body(response);
            }

            // 处理文件分类
            FileCategory fileCategory = null;
            if (category != null && !category.isEmpty()) {
                try {
                    fileCategory = FileCategory.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // 使用自动分类
                }
            }

            // 存储文件
            FileInfo fileInfo = fileStorageService.storeFile(file, fileCategory, uploadUser);
            fileInfo.setDownloadUrl("/api/v1/files/download/" + fileInfo.getFilename());

            // 构建成功响应
            response.put("success", true);
            response.put("code", 200);
            response.put("message", "文件上传成功");
            response.put("data", fileInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("code", 500);
            response.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 软件端 - 多文件上传接口
     */
    @PostMapping(value = "/v1/files/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadMultipleFilesApi(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "uploadUser", defaultValue = "system") String uploadUser,
            @RequestParam(value = "category", required = false) String category) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证文件数组
            if (files == null || files.length == 0) {
                response.put("success", false);
                response.put("code", 400);
                response.put("message", "请选择要上传的文件");
                return ResponseEntity.badRequest().body(response);
            }

            // 处理文件分类
            FileCategory fileCategory = null;
            if (category != null && !category.isEmpty()) {
                try {
                    fileCategory = FileCategory.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // 使用自动分类
                }
            }

            List<FileInfo> successFiles = new ArrayList<>();
            List<Map<String, Object>> failedFiles = new ArrayList<>();

            // 批量处理文件
            for (MultipartFile file : files) {
                try {
                    if (file.isEmpty()) {
                        Map<String, Object> failedFile = new HashMap<>();
                        failedFile.put("filename", file.getOriginalFilename());
                        failedFile.put("reason", "文件为空");
                        failedFiles.add(failedFile);
                        continue;
                    }

                    // 文件大小验证 - 改为最大2GB
                    if (file.getSize() > MAX_FILE_SIZE) {
                        Map<String, Object> failedFile = new HashMap<>();
                        failedFile.put("filename", file.getOriginalFilename());
                        failedFile.put("reason", "文件大小不能超过 2GB");
                        failedFiles.add(failedFile);
                        continue;
                    }

                    FileInfo fileInfo = fileStorageService.storeFile(file, fileCategory, uploadUser);
                    fileInfo.setDownloadUrl("/api/v1/files/download/" + fileInfo.getFilename());
                    successFiles.add(fileInfo);

                } catch (Exception e) {
                    Map<String, Object> failedFile = new HashMap<>();
                    failedFile.put("filename", file.getOriginalFilename());
                    failedFile.put("reason", e.getMessage());
                    failedFiles.add(failedFile);
                }
            }

            // 构建响应
            response.put("success", true);
            response.put("code", 200);
            response.put("message", String.format("批量上传完成，成功%d个，失败%d个",
                    successFiles.size(), failedFiles.size()));
            response.put("data", Map.of(
                    "successFiles", successFiles,
                    "failedFiles", failedFiles,
                    "totalCount", files.length,
                    "successCount", successFiles.size(),
                    "failedCount", failedFiles.size()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("code", 500);
            response.put("message", "批量上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 软件端 - 文件下载接口
     */
    @GetMapping("/v1/files/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFileApi(@PathVariable String fileName,
                                                    HttpServletRequest request) {
        return handleFileDownload(fileName, request);
    }

    /**
     * 软件端 - 获取文件列表接口
     */
    @GetMapping("/v1/files")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFilesApi(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<FileInfo> files;

            if (category != null && !category.isEmpty()) {
                try {
                    FileCategory fileCategory = FileCategory.valueOf(category.toUpperCase());
                    files = fileStorageService.getFilesByCategory(fileCategory);
                } catch (IllegalArgumentException e) {
                    files = fileStorageService.getAllFiles();
                }
            } else {
                files = fileStorageService.getAllFiles();
            }

            // 设置下载URL
            files.forEach(file ->
                    file.setDownloadUrl("/api/v1/files/download/" + file.getFilename()));

            // 分页处理（简化版）
            int total = files.size();
            int start = Math.min(page * size, total);
            int end = Math.min((page + 1) * size, total);
            List<FileInfo> pagedFiles = files.subList(start, end);

            response.put("success", true);
            response.put("code", 200);
            response.put("data", Map.of(
                    "files", pagedFiles,
                    "pagination", Map.of(
                            "page", page,
                            "size", size,
                            "total", total,
                            "totalPages", (int) Math.ceil((double) total / size)
                    )
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("code", 500);
            response.put("message", "获取文件列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 软件端 - 删除文件接口
     */
    @DeleteMapping("/v1/files/{fileName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFileApi(@PathVariable String fileName) {

        Map<String, Object> response = new HashMap<>();

        try {
            boolean deleted = fileStorageService.deleteFile(fileName);

            if (deleted) {
                response.put("success", true);
                response.put("code", 200);
                response.put("message", "文件删除成功");
            } else {
                response.put("success", false);
                response.put("code", 404);
                response.put("message", "文件未找到");
            }

            return deleted ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("code", 500);
            response.put("message", "文件删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 软件端 - 获取文件信息接口
     */
    @GetMapping("/v1/files/{fileName}/info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFileInfoApi(@PathVariable String fileName) {

        Map<String, Object> response = new HashMap<>();

        try {
            FileInfo fileInfo = fileStorageService.getFileInfo(fileName);

            if (fileInfo != null) {
                fileInfo.setDownloadUrl("/api/v1/files/download/" + fileInfo.getFilename());

                response.put("success", true);
                response.put("code", 200);
                response.put("data", fileInfo);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("code", 404);
                response.put("message", "文件未找到");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("code", 500);
            response.put("message", "获取文件信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 统一的文件上传处理方法
     */
    private String handleFileUpload(MultipartFile file, RedirectAttributes redirectAttributes, String redirectUrl) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "请选择要上传的文件");
            return redirectUrl;
        }

        // 客户端验证 - 改为最大2GB
        if (file.getSize() > MAX_FILE_SIZE) {
            redirectAttributes.addFlashAttribute("message",
                    "文件太大！请上传小于 2GB 的文件。");
            return redirectUrl;
        }

        try {
            FileInfo fileInfo = fileStorageService.storeFile(file, null, "web_user");
            redirectAttributes.addFlashAttribute("message",
                    "文件上传成功: " + fileInfo.getOriginalFilename());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message",
                    "文件上传失败: " + e.getMessage());
        }

        return redirectUrl;
    }

    /**
     * 统一的文件下载处理方法
     */
    private ResponseEntity<Resource> handleFileDownload(String fileName, HttpServletRequest request) {
        try {
            System.out.println("开始下载文件: " + fileName);

            // 加载文件作为资源
            Resource resource = fileStorageService.loadFileAsResource(fileName);

            // 尝试确定文件的内容类型
            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                System.out.println("无法确定文件类型: " + ex.getMessage());
            }

            // 如果无法确定类型，使用默认值
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            System.out.println("文件类型: " + contentType);

            // 获取原始文件名（去掉时间戳前缀）
            String originalFileName = fileName.contains("_") ?
                    fileName.substring(fileName.indexOf("_") + 1) : fileName;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + originalFileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            System.err.println("下载文件时出错: " + fileName + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== 原有的REST API接口（保持兼容） ====================

    @GetMapping("/files/category/{category}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFilesByCategoryApi(@PathVariable String category) {
        Map<String, Object> response = new HashMap<>();
        try {
            FileCategory fileCategory = FileCategory.valueOf(category.toUpperCase());
            List<FileInfo> files = fileStorageService.getFilesByCategory(fileCategory);
            files.forEach(file ->
                    file.setDownloadUrl("/api/download/" + file.getFilename()));

            response.put("success", true);
            response.put("code", 200);
            response.put("data", files);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("code", 500);
            response.put("message", "获取分类文件失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/categories/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCategoryStatsApi() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<FileCategory, Long> stats = fileStorageService.getCategoryStatistics();
            response.put("success", true);
            response.put("code", 200);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("code", 500);
            response.put("message", "获取分类统计失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", new Date());
        response.put("service", "File Manager API");
        response.put("maxFileSize", MAX_FILE_SIZE);
        response.put("maxFileSizeFormatted", formatFileSize(MAX_FILE_SIZE));
        return ResponseEntity.ok(response);
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
}