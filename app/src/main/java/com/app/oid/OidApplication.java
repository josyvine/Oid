package com.oid.crash;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * The custom Application class for Oid Crash.
 * Initializes global configurations and notification channels 
 * required for background monitoring services.
 */
public class OidApplication extends Application {

    // Unique ID for the persistent monitoring notification channel
    public static final String CHANNEL_ID = "oid_monitor_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the notification channel required for the Foreground Service
        createNotificationChannel();
    }

    /**
     * Creates a Notification Channel for the background monitor.
     * Required for Android O (API 26) and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notif_channel_name);
            String description = getString(R.string.notif_channel_desc);
            
            // IMPORTANCE_LOW: The notification is shown in the tray 
            // but does not make a sound, which is ideal for a persistent service.
            int importance = NotificationManager.IMPORTANCE_LOW;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}