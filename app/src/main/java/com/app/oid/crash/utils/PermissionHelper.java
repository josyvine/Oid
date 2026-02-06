package com.oid.crash.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;

import com.oid.crash.services.OidAccessibilityService;

/**
 * Utility class to verify system-level permissions required for monitoring.
 * Specifically checks for Accessibility and Usage Statistics access.
 */
public class PermissionHelper {

    /**
     * Checks if the OidAccessibilityService is currently enabled in the system settings.
     * 
     * @param context App context
     * @return true if the service is active and enabled by the user.
     */
    public static boolean isAccessibilityServiceEnabled(Context context) {
        int accessibilityEnabled = 0;
        final String service = context.getPackageName() + "/" + OidAccessibilityService.class.getCanonicalName();
        
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // Setting not found, assume disabled
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the app has been granted 'Usage Access' by the user.
     * This is required to identify which app is currently in the foreground.
     */
    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 
                Process.myUid(), context.getPackageName());
        
        if (mode == AppOpsManager.MODE_DEFAULT) {
            // On some devices, default might be allowed, but we check specifically for permission
            return context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Checks for Notification permission (Required for Android 13/API 33+).
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not required on older versions
    }

    /**
     * Checks if the app has the necessary permissions to run in Power Mode (ADB logic).
     * Power Mode technically requires the ADB loopback to be authenticated via the SettingsFragment.
     */
    public static boolean isPowerModeReady(Context context) {
        AppDatabaseHelper db = AppDatabaseHelper.getInstance(context);
        String ip = db.getAdbIp();
        // If IP is set and not empty, we assume the user has at least attempted pairing.
        return !TextUtils.isEmpty(ip);
    }
}