package com.oid.crash.services;

import android.accessibilityservice.AccessibilityService;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.oid.crash.utils.AppDatabaseHelper;
import com.oid.crash.utils.CrashLogGenerator;

import java.util.List;

/**
 * Accessibility Service component for Oid Crash.
 * This service detects System Crash Dialogs and ANR popups in Normal Mode.
 */
public class OidAccessibilityService extends AccessibilityService {

    private static final String TAG = "OidAccessibility";
    private AppDatabaseHelper db;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        db = AppDatabaseHelper.getInstance(this);
        Log.d(TAG, "Accessibility Service Connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We only care about window changes (dialogs appearing)
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName == null) return;

            // System dialogs often come from "android" or "com.android.systemui"
            if (packageName.equals("android") || packageName.equals("com.android.systemui")) {
                inspectSystemWindow(event);
            }
        }
    }

    /**
     * Inspects the content of a system window to see if it's a crash or ANR dialog.
     */
    private void inspectSystemWindow(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // Keywords indicating a crash or ANR in various Android versions
        String[] crashKeywords = {"stopped", "responding", "closed", "crash", "close app"};
        
        // Search for these keywords in the current dialog text
        for (String keyword : crashKeywords) {
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(keyword);
            if (nodes != null && !nodes.isEmpty()) {
                handleDetectedCrash(rootNode);
                break;
            }
        }
        rootNode.recycle();
    }

    /**
     * Parses the text in the dialog to identify which app crashed 
     * and triggers the report generation.
     */
    private void handleDetectedCrash(AccessibilityNodeInfo rootNode) {
        // Usually, the dialog contains the app's name. 
        // We attempt to find a package name from the active node or history.
        
        // In Normal Mode, we capture the last 500 lines of logcat as "Partial Logs"
        String partialLogs = captureRecentLogs();
        
        // Retrieve all text from the dialog to identify the app
        String dialogText = getAllText(rootNode);
        Log.e(TAG, "Crash Dialog Detected: " + dialogText);

        // Cross-reference with our target apps
        for (String targetPkg : db.getTargetPackages()) {
            String appName = getAppName(targetPkg);
            
            // If the dialog text contains the name of one of our target apps
            if (dialogText.contains(appName) || dialogText.contains(targetPkg)) {
                Log.i(TAG, "Confirmed crash for target: " + targetPkg);
                
                // Trigger report generation via the Generator utility
                CrashLogGenerator.createReport(
                        this, 
                        appName, 
                        targetPkg, 
                        "--- NORMAL MODE CAPTURE ---\n" +
                        "Event: System Crash/ANR Dialog Detected\n" +
                        "Dialog Content: " + dialogText + "\n\n" +
                        "--- RECENT LOGS ---\n" + partialLogs
                );
            }
        }
    }

    private String captureRecentLogs() {
        StringBuilder log = new StringBuilder();
        try {
            // Capture last 500 lines of logcat
            Process process = Runtime.getRuntime().exec("logcat -d -t 500");
            java.io.BufferedReader bufferedReader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line).append("\n");
            }
        } catch (Exception e) {
            log.append("Error capturing logs: ").append(e.getMessage());
        }
        return log.toString();
    }

    private String getAppName(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (Exception e) {
            return packageName;
        }
    }

    private String getAllText(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        if (node.getText() != null) {
            sb.append(node.getText()).append(" ");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            sb.append(getAllText(node.getChild(i)));
        }
        return sb.toString();
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility Service Interrupted");
    }
}