package com.djwsj.filemanager.controller;

import com.djwsj.filemanager.entity.FileInfo;
import com.djwsj.filemanager.enums.FileCategory;
import com.djwsj.filemanager.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/")
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
                    file.setDownloadUrl("/download/" + file.getFilename()));

            // 获取分类统计
            Map<FileCategory, Long> categoryStats = fileStorageService.getCategoryStatistics();

            model.addAttribute("files", files != null ? files : new ArrayList<>());
            model.addAttribute("categories", FileCategory.values());
            model.addAttribute("categoryStats", categoryStats != null ? categoryStats : new HashMap<>());
            model.addAttribute("currentCategory", category);

        } catch (IOException e) {
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
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "请选择要上传的文件");
            return "redirect:/";
        }

        // 客户端验证（可选，服务端验证是必须的）
        if (file.getSize() > 2L * 1024 * 1024 * 1024) { // 2GB
            redirectAttributes.addFlashAttribute("message",
                    "文件太大！请上传小于 2GB 的文件。");
            return "redirect:/";
        }

        try {
            FileInfo fileInfo = fileStorageService.storeFile(file);
            redirectAttributes.addFlashAttribute("message",
                    "文件上传成功: " + fileInfo.getOriginalFilename());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message",
                    "文件上传失败: " + e.getMessage());
        }

        return "redirect:/";
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
        return "redirect:/";
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName,
                                                 HttpServletRequest request) {
        try {
            System.out.println("开始下载文件: " + fileName);

            // 加载文件作为资源
            Resource resource = (Resource) fileStorageService.loadFileAsResource(fileName);

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

    // REST API - 按分类获取文件
    @GetMapping("/api/files/category/{category}")
    @ResponseBody
    public List<FileInfo> getFilesByCategoryApi(@PathVariable String category) throws IOException {
        FileCategory fileCategory = FileCategory.valueOf(category.toUpperCase());
        List<FileInfo> files = fileStorageService.getFilesByCategory(fileCategory);
        files.forEach(file ->
                file.setDownloadUrl("/download/" + file.getFilename()));
        return files;
    }

    // REST API - 获取分类统计
    @GetMapping("/api/categories/stats")
    @ResponseBody
    public Map<FileCategory, Long> getCategoryStatsApi() throws IOException {
        return fileStorageService.getCategoryStatistics();
    }
}