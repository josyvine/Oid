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
 * Updated: Adds Pulse tracking (dumpsys), Broad java.lang detection, and Auto-Reconnect.
 */
public class AdbMonitorThread extends Thread {

    private static final String TAG = "AdbMonitorThread";
    private final Context context;
    private final AppDatabaseHelper db;
    private final CrashMonitorService service;
    private boolean isRunning = true;
    private Process adbProcess;
    private long lastPulseTime = 0;

    public AdbMonitorThread(Context context, CrashMonitorService service) {
        this.context = context;
        this.service = service;
        this.db = AppDatabaseHelper.getInstance(context);
    }

    @Override
    public void run() {
        Log.i(TAG, "Power Mode Engine Started [Pulse + Broad Detection Active]");

        while (isRunning) {
            try {
                // Connect to the internal port unlocked via Bugjaeger
                // -b crash: specifically targets the system crash buffer
                String[] cmd = {"logcat", "-b", "crash", "-v", "time"};
                adbProcess = Runtime.getRuntime().exec(cmd);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(adbProcess.getInputStream()));

                String line;
                StringBuilder stackTraceBuilder = new StringBuilder();
                String currentCrashingPkg = null;
                boolean capturingTrace = false;

                while (isRunning && (line = reader.readLine()) != null) {
                    
                    // --- 1. PERIODIC PULSE CHECK ---
                    // Runs every 2 seconds to update the Graph UI
                    if (System.currentTimeMillis() - lastPulseTime > 2000) {
                        performActivityPulse();
                        lastPulseTime = System.currentTimeMillis();
                    }

                    // --- 2. BROAD DETECTION LOGIC ---
                    // Catching standard Fatal signals and specific java.lang errors
                    if (line.contains("FATAL EXCEPTION") || 
                        line.contains("java.lang.") || 
                        line.contains("NullPointerException") ||
                        line.contains("RuntimeException")) {
                        
                        capturingTrace = true;
                        stackTraceBuilder = new StringBuilder();
                        stackTraceBuilder.append(line).append("\n");
                        
                        currentCrashingPkg = parsePackageFromLine(line);
                        continue;
                    }

                    if (capturingTrace) {
                        stackTraceBuilder.append(line).append("\n");

                        // Stop capturing if we see a new timestamp or empty line
                        if (line.trim().isEmpty() || isNewLogEntry(line)) {
                            capturingTrace = false;
                            
                            if (currentCrashingPkg != null && db.isAppTargeted(currentCrashingPkg)) {
                                // SEND RED PULSE (Status 3)
                                service.onPulseReceived(3);

                                String appName = getAppName(currentCrashingPkg);
                                Log.e(TAG, "JAVA.LANG CRASH CAPTURED: " + currentCrashingPkg);
                                
                                CrashLogGenerator.createReport(
                                        context,
                                        appName,
                                        currentCrashingPkg,
                                        "--- POWER MODE (ADB) FULL STACKTRACE ---\n" +
                                        stackTraceBuilder.toString()
                                );
                            }
                            currentCrashingPkg = null;
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Power Mode Port Error (Is 5555 closed?): " + e.getMessage());
                // Wait 5 seconds before trying to reconnect (Auto-Reconnect logic)
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Uses 'dumpsys activity' to check if target app is foreground.
     * Logic: Status 1 = Active (Green), Status 2 = Background (Gray).
     */
    private void performActivityPulse() {
        try {
            Process p = Runtime.getRuntime().exec("dumpsys activity activities");
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;
            boolean targetInForeground = false;
            Set<String> targets = db.getTargetPackages();

            while ((l = r.readLine()) != null) {
                // Focus only on the currently visible activity
                if (l.contains("mResumedActivity") || l.contains("topResumedActivity")) {
                    for (String pkg : targets) {
                        if (l.contains(pkg)) {
                            targetInForeground = true;
                            break;
                        }
                    }
                }
            }
            
            // Update the Graph through the Service
            service.onPulseReceived(targetInForeground ? 1 : 2);
            
            r.close();
            p.destroy();
        } catch (Exception ignored) {}
    }

    private String parsePackageFromLine(String line) {
        if (line.contains("Process: ")) {
            try {
                int start = line.indexOf("Process: ") + 9;
                int end = line.indexOf(",", start);
                if (end == -1) end = line.length();
                return line.substring(start, end).trim();
            } catch (Exception e) { return null; }
        }
        return null;
    }

    private boolean isNewLogEntry(String line) {
        return line.matches("^\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.*");
    }

    private String getAppName(String packageName) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (Exception e) { return packageName; }
    }

    public void stopMonitoring() {
        isRunning = false;
        if (adbProcess != null) {
            adbProcess.destroy();
        }
        this.interrupt();
    }
}