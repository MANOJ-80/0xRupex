package com.rupex.app.util;

import android.content.Context;
import android.util.Log;

import com.rupex.app.data.local.RupexDatabase;
import com.rupex.app.data.local.entity.ActivityLog;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Utility class for logging app activities
 */
public class ActivityLogger {

    private static final String TAG = "ActivityLogger";
    private static final int MAX_LOGS = 100; // Keep last 100 logs

    /**
     * Log that something was captured (SMS/Notification)
     */
    public static void logCaptured(Context context, String source, String message, Double amount, String merchant) {
        ActivityLog log = new ActivityLog();
        log.setType("captured");
        log.setSource(source);
        log.setMessage(message);
        log.setAmount(amount);
        log.setMerchant(merchant);
        log.setTimestamp(System.currentTimeMillis());
        
        insertLog(context, log);
    }

    /**
     * Log that a transaction was added
     */
    public static void logAdded(Context context, String source, String message, Double amount, String merchant) {
        ActivityLog log = new ActivityLog();
        log.setType("added");
        log.setSource(source);
        log.setMessage(message);
        log.setAmount(amount);
        log.setMerchant(merchant);
        log.setTimestamp(System.currentTimeMillis());
        
        insertLog(context, log);
    }

    /**
     * Log that something was rejected
     */
    public static void logRejected(Context context, String source, String message, String reason, Double amount, String merchant) {
        ActivityLog log = new ActivityLog();
        log.setType("rejected");
        log.setSource(source);
        log.setMessage(message);
        log.setReason(reason);
        log.setAmount(amount);
        log.setMerchant(merchant);
        log.setTimestamp(System.currentTimeMillis());
        
        insertLog(context, log);
    }

    private static void insertLog(Context context, ActivityLog log) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                RupexDatabase db = RupexDatabase.getInstance(context);
                db.activityLogDao().insert(log);
                
                // Clean up old logs (keep only last MAX_LOGS)
                List<ActivityLog> allLogs = db.activityLogDao().getRecentLogsSync(MAX_LOGS + 50);
                if (allLogs.size() > MAX_LOGS) {
                    long cutoffTime = allLogs.get(MAX_LOGS - 1).getTimestamp();
                    db.activityLogDao().deleteOldLogs(cutoffTime);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error logging activity", e);
            }
        });
    }
}

