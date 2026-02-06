package com.oid.crash.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Responsible for constructing the professional .txt crash report.
 * Strictly follows the format specified in the project requirements.
 */
public class CrashLogGenerator {

    private static final String TAG = "CrashLogGenerator";
    private static final String BASE_DIRECTORY = "reports";

    /**
     * Main method to generate and save a crash report.
     * 
     * @param context App context
     * @param appName Display name of the crashed app
     * @param packageName Package ID of the crashed app
     * @param stackTrace Raw stack trace string (from Normal or Power mode)
     */
    public static void createReport(Context context, String appName, String packageName, String stackTrace) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
        String fileName = appName + "-" + packageName + "-" + timestamp + ".txt";
        
        StringBuilder report = new StringBuilder();

        // ----------------------------------------
        // HEADER
        // ----------------------------------------
        report.append("----------------------------------------\n");
        report.append("Oid Crash - Crash Report\n");
        report.append("----------------------------------------\n\n");

        // APP INFO
        report.append("App Name: ").append(appName).append("\n");
        report.append("Package Name: ").append(packageName).append("\n");
        report.append("Version: ").append(getAppVersion(context, packageName)).append("\n");
        
        // DEVICE INFO
        report.append("Device: ").append(Build.DEVICE).append("\n");
        report.append("Brand: ").append(Build.BRAND).append("\n");
        report.append("Model: ").append(Build.MODEL).append("\n");
        report.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        report.append("Security Patch: ").append(Build.VERSION.SECURITY_PATCH).append("\n");
        report.append("Time: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");

        // ----------------------------------------
        // CRASH SUMMARY (Extracted from stacktrace if possible)
        // ----------------------------------------
        report.append("----------------------------------------\n");
        report.append("CRASH SUMMARY\n");
        report.append("----------------------------------------\n\n");
        
        String exceptionLine = extractException(stackTrace);
        report.append("Exception: ").append(exceptionLine).append("\n");
        report.append("Message: ").append(extractMessage(stackTrace)).append("\n");
        report.append("Thread: ").append(Thread.currentThread().getName()).append("\n\n");

        // ----------------------------------------
        // STACK TRACE
        // ----------------------------------------
        report.append("----------------------------------------\n");
        report.append("STACK TRACE\n");
        report.append("----------------------------------------\n\n");
        report.append(stackTrace).append("\n\n");

        // ----------------------------------------
        // SYSTEM CONTEXT
        // ----------------------------------------
        report.append("----------------------------------------\n");
        report.append("SYSTEM CONTEXT\n");
        report.append("----------------------------------------\n\n");
        
        report.append("CPU Usage: ").append(getCPUInfo()).append("\n");
        report.append("Memory: ").append(getMemoryInfo(context)).append("\n");
        report.append("Foreground Process: ").append(packageName).append("\n");
        report.append("Running Services: ").append(getRunningServicesCount(context)).append("\n\n");

        report.append("----------------------------------------\n");
        report.append("END OF REPORT\n");
        report.append("----------------------------------------\n");

        saveToFile(context, packageName, fileName, report.toString());
    }

    private static void saveToFile(Context context, String packageName, String fileName, String content) {
        // Path: /Android/data/com.oid.crash/files/reports/<package_name>/
        File dir = new File(context.getExternalFilesDir(BASE_DIRECTORY), packageName);
        
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
            Log.d(TAG, "Report saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save report", e);
        }
    }

    private static String getAppVersion(Context context, String packageName) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return pInfo.versionName + " (" + pInfo.getLongVersionCode() + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown";
        }
    }

    private static String extractException(String trace) {
        if (trace == null || trace.isEmpty()) return "N/A";
        String[] lines = trace.split("\n");
        for (String line : lines) {
            if (line.contains("Exception") || line.contains("Error")) return line.trim();
        }
        return "Unknown Exception";
    }

    private static String extractMessage(String trace) {
        if (trace == null || trace.isEmpty()) return "N/A";
        int firstColon = trace.indexOf(":");
        if (firstColon != -1 && firstColon < trace.indexOf("\n")) {
            return trace.substring(firstColon + 1, trace.indexOf("\n")).trim();
        }
        return "No message details found.";
    }

    private static String getMemoryInfo(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;
        long totalMegs = mi.totalMem / 1048576L;
        return availableMegs + "MB free / " + totalMegs + "MB total";
    }

    private static String getCPUInfo() {
        // Simplified CPU indicator as modern Android restricts /proc/stat
        return Build.SUPPORTED_ABIS[0] + " Architecture";
    }

    private static int getRunningServicesCount(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am.getRunningAppProcesses().size();
    }
}