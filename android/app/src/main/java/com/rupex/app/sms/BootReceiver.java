package com.rupex.app.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rupex.app.sync.SyncManager;

/**
 * Receiver to restart sync service after device reboot
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            Log.i(TAG, "Device booted, scheduling periodic sync");
            
            // Schedule periodic sync work
            SyncManager.schedulePeriodicSync(context);
        }
    }
}
