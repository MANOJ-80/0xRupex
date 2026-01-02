package com.rupex.app.sync;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.rupex.app.R;
import com.rupex.app.RupexApplication;
import com.rupex.app.ui.MainActivity;

/**
 * Foreground service for sync operations (required for Android 12+)
 */
public class SyncService extends Service {

    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create notification for foreground service
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        // Perform sync
        new Thread(() -> {
            // Sync logic here (or trigger WorkManager)
            SyncManager.scheduleSyncNow(getApplicationContext());
            
            // Stop service after scheduling
            stopForeground(true);
            stopSelf();
        }).start();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, RupexApplication.CHANNEL_SYNC)
                .setContentTitle("0xRupex")
                .setContentText("Syncing transactions...")
                .setSmallIcon(R.drawable.ic_sync)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
