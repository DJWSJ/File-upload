package com.djwsj.filemanager.enums;

// src/main/java/com/djwsj/filemanager/enums/FileCategory.java

public enum FileCategory {
    DOCUMENT("文档", new String[]{"pdf", "doc", "docx", "txt", "ppt", "pptx", "xls", "xlsx", "md"}, "#2196F3"),
    IMAGE("图片", new String[]{"jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "ico"}, "#4CAF50"),
    VIDEO("视频", new String[]{"mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"}, "#FF9800"),
    AUDIO("音频", new String[]{"mp3", "wav", "ogg", "flac", "aac", "m4a"}, "#9C27B0"),
    ARCHIVE("压缩包", new String[]{"zip", "rar", "7z", "tar", "gz", "bz2"}, "#795548"),
    CODE("代码", new String[]{"java", "js", "html", "css", "py", "cpp", "c", "h", "xml", "json"}, "#607D8B"),
    EXECUTABLE("可执行文件", new String[]{"exe", "msi", "dmg", "pkg", "deb", "rpm"}, "#F44336"),
    OTHER("其他", new String[]{}, "#9E9E9E");

    private final String displayName;
    private final String[] extensions;
    private final String color;

    FileCategory(String displayName, String[] extensions, String color) {
        this.displayName = displayName;
        this.extensions = extensions;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public String getColor() {
        return color;
    }

    public static FileCategory fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return OTHER;
        }

        String ext = extension.toLowerCase();
        for (FileCategory category : values()) {
            for (String categoryExt : category.extensions) {
                if (categoryExt.equals(ext)) {
                    return category;
                }
            }
        }
        return OTHER;
    }

    public static FileCategory fromFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return OTHER;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            String extension = filename.substring(lastDotIndex + 1).toLowerCase();
            return fromExtension(extension);
        }
        return OTHER;
    }
}