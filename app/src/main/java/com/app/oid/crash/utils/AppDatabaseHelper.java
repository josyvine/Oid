package com.oid.crash.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages local persistent storage for Oid Crash.
 * Handles:
 * 1) Selected Target App package names.
 * 2) Monitoring Mode (Normal vs Power).
 * 3) Wireless ADB pairing details.
 */
public class AppDatabaseHelper {

    private static final String PREF_NAME = "oid_crash_prefs";
    private static final String KEY_TARGET_PACKAGES = "target_packages";
    private static final String KEY_MONITOR_MODE = "monitor_mode"; // 0 for Normal, 1 for Power
    private static final String KEY_ADB_IP = "adb_ip";
    private static final String KEY_ADB_PORT = "adb_port";
    private static final String KEY_SERVICE_ACTIVE = "service_active";

    private static AppDatabaseHelper instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private AppDatabaseHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized AppDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AppDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Saves the set of package names the user wants to monitor.
     */
    public void saveTargetPackages(Set<String> packages) {
        String json = gson.toJson(packages);
        prefs.edit().putString(KEY_TARGET_PACKAGES, json).apply();
    }

    /**
     * Retrieves the set of package names currently being monitored.
     */
    public Set<String> getTargetPackages() {
        String json = prefs.getString(KEY_TARGET_PACKAGES, null);
        if (json == null) {
            return new HashSet<>();
        }
        Type type = new TypeToken<HashSet<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    /**
     * Checks if a specific app is in the user's target list.
     */
    public boolean isAppTargeted(String packageName) {
        return getTargetPackages().contains(packageName);
    }

    /**
     * Saves the monitoring mode: 0 for Normal, 1 for Power Mode.
     */
    public void setMonitorMode(int mode) {
        prefs.edit().putInt(KEY_MONITOR_MODE, mode).apply();
    }

    public int getMonitorMode() {
        return prefs.getInt(KEY_MONITOR_MODE, 0); // Defaults to Normal Mode
    }

    /**
     * Tracks whether the background service should be active.
     */
    public void setServiceActive(boolean active) {
        prefs.edit().putBoolean(KEY_SERVICE_ACTIVE, active).apply();
    }

    public boolean isServiceActive() {
        return prefs.getBoolean(KEY_SERVICE_ACTIVE, false);
    }

    /**
     * Storage for Wireless ADB connection details.
     */
    public void saveAdbConnection(String ip, int port) {
        prefs.edit()
                .putString(KEY_ADB_IP, ip)
                .putInt(KEY_ADB_PORT, port)
                .apply();
    }

    public String getAdbIp() {
        return prefs.getString(KEY_ADB_IP, "");
    }

    public int getAdbPort() {
        return prefs.getInt(KEY_ADB_PORT, 5555);
    }

    /**
     * Completely clear settings (useful for resets).
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}