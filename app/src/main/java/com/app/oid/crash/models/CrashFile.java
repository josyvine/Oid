package com.oid.crash.models;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Data model representing a saved crash report file (.txt).
 * Used in the History screen to display report metadata.
 */
public class CrashFile {

    private final String fileName;
    private final String filePath;
    private final String appName;
    private final long timestamp;
    private final long fileSize;

    public CrashFile(File file) {
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.fileSize = file.length();
        this.timestamp = file.lastModified();
        
        // Logic to extract App Name from filename: <appname>-<package>-<timestamp>.txt
        this.appName = extractAppName(fileName);
    }

    private String extractAppName(String name) {
        try {
            if (name.contains("-")) {
                return name.split("-")[0];
            }
        } catch (Exception e) {
            return "Unknown App";
        }
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getAppName() {
        return appName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getFileSize() {
        return fileSize;
    }

    /**
     * Converts the raw timestamp into a human-readable date for the UI.
     */
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Converts the file size into a readable string (KB/MB).
     */
    public String getReadableFileSize() {
        if (fileSize <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#")
                .format(fileSize / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}