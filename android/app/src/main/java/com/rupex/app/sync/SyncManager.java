package com.rupex.app.sync;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Manages background sync scheduling
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String WORK_SYNC_NOW = "sync_now";
    private static final String WORK_PERIODIC_SYNC = "periodic_sync";

    /**
     * Schedule immediate sync (when new SMS is received)
     */
    public static void scheduleSyncNow(Context context) {
        Log.d(TAG, "Scheduling immediate sync");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .addTag(WORK_SYNC_NOW)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        WORK_SYNC_NOW,
                        ExistingWorkPolicy.REPLACE,
                        syncRequest
                );
    }

    /**
     * Schedule periodic background sync (every 15 minutes)
     */
    public static void schedulePeriodicSync(Context context) {
        Log.d(TAG, "Scheduling periodic sync");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15, TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .addTag(WORK_PERIODIC_SYNC)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        WORK_PERIODIC_SYNC,
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncRequest
                );
    }

    /**
     * Cancel all sync work
     */
    public static void cancelAllSync(Context context) {
        WorkManager.getInstance(context).cancelAllWork();
    }
}
