package com.wenmin.prometheus.module.distribute.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SoftwareDownloadVO {

    private String downloadId;
    private String status; // checking, running, completed, failed
    private int totalComponents;
    private int completedComponents;
    private int skippedComponents;
    private int totalFiles;
    private int downloadedFiles;
    private int failedFiles;
    private int skippedFiles;
    private String currentComponent;
    private String currentFile;
    private String message;
    private List<ComponentDownloadResult> results = new ArrayList<>();

    @Data
    public static class ComponentDownloadResult {
        private String name;
        private String displayName;
        private String previousVersion;
        private String newVersion;
        private String status; // success, partial, failed, skipped
        private int filesDownloaded;
        private int filesFailed;
    }
}
