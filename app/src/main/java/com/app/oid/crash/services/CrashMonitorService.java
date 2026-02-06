package com.oid.crash.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.oid.crash.MainActivity;
import com.oid.crash.OidApplication;
import com.oid.crash.R;
import com.oid.crash.utils.AppDatabaseHelper;
import com.oid.crash.utils.CrashLogGenerator;

import java.util.Set;

/**
 * The primary Background Service for Oid Crash.
 * This service stays alive to monitor app transitions and 
 * manage the crash detection lifecycle.
 */
public class CrashMonitorService extends Service {

    private static final String TAG = "CrashMonitorService";
    private static final int NOTIF_ID = 1001;
    private static final long CHECK_INTERVAL = 2000; // Check foreground app every 2 seconds

    private Handler handler;
    private Runnable monitorRunnable;
    private String lastForegroundPackage = "";
    private AppDatabaseHelper db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = AppDatabaseHelper.getInstance(this);
        handler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Starting...");

        // 1. Start as Foreground Service with Persistent Notification
        startForeground(NOTIF_ID, createNotification());

        // 2. Begin the Foreground App Tracking Loop
        startMonitoringLoop();

        return START_STICKY; // Ensure service restarts if killed by system
    }

    /**
     * Creates the mandatory persistent notification for the Foreground Service.
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE
        );

        String statusText = getString(R.string.notif_text);
        if (db.getMonitorMode() == 1) {
            statusText = "Oid Crash: Monitoring in POWER MODE";
        }

        return new NotificationCompat.Builder(this, OidApplication.CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(statusText)
                .setSmallIcon(R.drawable.oid)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * Logic to track which app the user is currently interacting with.
     * Uses UsageStatsManager to identify foreground processes.
     */
    private void startMonitoringLoop() {
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                String currentApp = getForegroundPackageName();
                
                if (!currentApp.equals(lastForegroundPackage)) {
                    lastForegroundPackage = currentApp;
                    Log.d(TAG, "Foreground App Changed: " + currentApp);
                    
                    // If the new foreground app is a target, prepare for potential logs
                    if (db.isAppTargeted(currentApp)) {
                        Log.i(TAG, "Now monitoring target app: " + currentApp);
                    }
                }

                // Repeat every 2 seconds
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.post(monitorRunnable);
    }

    /**
     * Retrieves the package name of the app currently in the foreground.
     * Requires PACKAGE_USAGE_STATS permission.
     */
    private String getForegroundPackageName() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        UsageEvents events = usm.queryEvents(time - 1000 * 60, time);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastPkg = "";
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPkg = event.getPackageName();
            }
        }
        return lastPkg;
    }

    /**
     * This method is called when OidAccessibilityService or the ADB Monitor 
     * detects a crash event.
     */
    public void onCrashDetected(String packageName, String appName, String errorDetail) {
        Set<String> targets = db.getTargetPackages();
        
        // Only generate report if the crashed app is in the user's selection
        if (targets.contains(packageName)) {
            Log.e(TAG, "ALERT: Crash detected in target app: " + packageName);
            
            // Trigger the generation of the professional .txt report
            CrashLogGenerator.createReport(this, appName, packageName, errorDetail);
        } else {
            Log.d(TAG, "Crash detected in non-target app: " + packageName + ". Ignoring.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && monitorRunnable != null) {
            handler.removeCallbacks(monitorRunnable);
        }
        Log.d(TAG, "Service Destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We use startService, not bindService
    }
}