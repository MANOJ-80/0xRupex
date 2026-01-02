package com.rupex.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.work.Configuration;
import androidx.work.WorkManager;

/**
 * Application class for 0xRupex
 * Initializes notification channels and WorkManager
 */
public class RupexApplication extends Application {

    public static final String CHANNEL_SYNC = "sync_channel";
    public static final String CHANNEL_ALERTS = "alerts_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Sync notifications (low priority, silent)
            NotificationChannel syncChannel = new NotificationChannel(
                    CHANNEL_SYNC,
                    getString(R.string.notification_channel_sync),
                    NotificationManager.IMPORTANCE_LOW
            );
            syncChannel.setDescription("Background transaction sync notifications");
            syncChannel.setShowBadge(false);
            manager.createNotificationChannel(syncChannel);

            // Alert notifications (high priority, with sound)
            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS,
                    getString(R.string.notification_channel_alerts),
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Budget alerts and important notifications");
            alertChannel.setShowBadge(true);
            manager.createNotificationChannel(alertChannel);
        }
    }
}
