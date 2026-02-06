package com.oid.crash.services;

import android.content.Context;
import android.util.Log;

import com.oid.crash.utils.AppDatabaseHelper;
import com.oid.crash.utils.CrashLogGenerator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * The core logic for Power Mode.
 * This thread executes local shell commands to monitor the system's crash buffer.
 * It provides the "Full Stacktrace" capability using Wireless ADB logic.
 */
public class AdbMonitorThread extends Thread {

    private static final String TAG = "AdbMonitorThread";
    private final Context context;
    private final AppDatabaseHelper db;
    private boolean isRunning = true;
    private Process adbProcess;

    public AdbMonitorThread(Context context) {
        this.context = context;
        this.db = AppDatabaseHelper.getInstance(context);
    }

    @Override
    public void run() {
        Log.i(TAG, "Power Mode Monitor Thread Started");

        while (isRunning) {
            try {
                // Command: logcat -b crash -v time
                // -b crash: Only look at the crash buffer (efficient)
                // -v time: Include timestamps
                String[] cmd = {"logcat", "-b", "crash", "-v", "time"};
                adbProcess = Runtime.getRuntime().exec(cmd);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(adbProcess.getInputStream()));

                String line;
                StringBuilder stackTraceBuilder = new StringBuilder();
                String currentCrashingPkg = null;
                boolean capturingTrace = false;

                while (isRunning && (line = reader.readLine()) != null) {
                    // Look for the start of a fatal exception
                    if (line.contains("FATAL EXCEPTION")) {
                        capturingTrace = true;
                        stackTraceBuilder = new StringBuilder();
                        stackTraceBuilder.append(line).append("\n");
                        
                        // Attempt to identify the package from the log line
                        currentCrashingPkg = parsePackageFromLine(line);
                        continue;
                    }

                    if (capturingTrace) {
                        stackTraceBuilder.append(line).append("\n");

                        // If we see an empty line or the start of a new log entry, 
                        // the stack trace for the current crash is likely finished.
                        if (line.trim().isEmpty() || isNewLogEntry(line)) {
                            capturingTrace = false;
                            
                            if (currentCrashingPkg != null && db.isAppTargeted(currentCrashingPkg)) {
                                String appName = getAppName(currentCrashingPkg);
                                
                                Log.e(TAG, "Power Mode: Stacktrace captured for " + currentCrashingPkg);
                                
                                // Generate the full professional report
                                CrashLogGenerator.createReport(
                                        context,
                                        appName,
                                        currentCrashingPkg,
                                        "--- POWER MODE (ADB) FULL CAPTURE ---\n" +
                                        stackTraceBuilder.toString()
                                );
                            }
                            currentCrashingPkg = null;
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "ADB Monitor Loop Error: " + e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Identifies the package name from a logcat crash line.
     * Example line: E/AndroidRuntime(1234): Process: com.whatsapp, PID: 1234
     */
    private String parsePackageFromLine(String line) {
        if (line.contains("Process: ")) {
            try {
                int start = line.indexOf("Process: ") + 9;
                int end = line.indexOf(",", start);
                if (end == -1) end = line.length();
                return line.substring(start, end).trim();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private boolean isNewLogEntry(String line) {
        // Logcat lines with -v time usually start with a date format (MM-DD HH:MM:SS)
        return line.matches("^\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.*");
    }

    private String getAppName(String packageName) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (Exception e) {
            return packageName;
        }
    }

    public void stopMonitoring() {
        isRunning = false;
        if (adbProcess != null) {
            adbProcess.destroy();
        }
        this.interrupt();
    }
}