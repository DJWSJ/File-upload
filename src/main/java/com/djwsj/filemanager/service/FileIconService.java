// src/main/java/com/djwsj/filemanager/service/FileIconService.java
package com.djwsj.filemanager.service;

import com.djwsj.filemanager.enums.FileCategory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FileIconService {

    private final Map<FileCategory, String> categoryColors = new HashMap<>();

    public FileIconService() {
        // 初始化分类颜色
        categoryColors.put(FileCategory.DOCUMENT, "#2196F3");
        categoryColors.put(FileCategory.IMAGE, "#4CAF50");
        categoryColors.put(FileCategory.VIDEO, "#FF9800");
        categoryColors.put(FileCategory.AUDIO, "#9C27B0");
        categoryColors.put(FileCategory.ARCHIVE, "#795548");
        categoryColors.put(FileCategory.CODE, "#607D8B");
        categoryColors.put(FileCategory.EXECUTABLE, "#F44336");
        categoryColors.put(FileCategory.OTHER, "#9E9E9E");
    }

    public String getCategoryColor(FileCategory category) {
        if (category == null) {
            return "#9E9E9E";
        }
        return categoryColors.getOrDefault(category, "#9E9E9E");
    }
}