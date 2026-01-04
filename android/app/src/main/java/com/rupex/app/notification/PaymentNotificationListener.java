package com.rupex.app.notification;

import android.app.Notification;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.rupex.app.data.local.RupexDatabase;
import com.rupex.app.data.local.entity.PendingTransaction;
import com.rupex.app.util.ActivityLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Listens to payment app notifications (GPay, PhonePe, Paytm, etc.)
 * and auto-logs transactions.
 */
public class PaymentNotificationListener extends NotificationListenerService {

    private static final String TAG = "PaymentNotification";

    // UPI App Package Names
    private static final String GPAY = "com.google.android.apps.nbu.paisa.user";
    private static final String PHONEPE = "com.phonepe.app";
    private static final String PAYTM = "net.one97.paytm";
    private static final String AMAZON_PAY = "in.amazon.mShop.android.shopping";
    private static final String BHIM = "in.org.npci.upiapp";
    private static final String CRED = "com.dreamplug.androidapp";
    private static final String WHATSAPP = "com.whatsapp"; // WhatsApp Pay

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        
        // Check if it's from a UPI payment app
        if (!isPaymentApp(packageName)) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }

        Log.d(TAG, "Payment notification from " + packageName);
        
        // Log that notification was captured
        ActivityLogger.logCaptured(getApplicationContext(), "notification", 
                "Notification from " + getAppName(packageName), null, null);
        
        // Try multiple methods to extract notification text
        String title = "";
        String content = "";
        
        // Method 1: Standard extras
        Bundle extras = notification.extras;
        if (extras != null) {
            title = getStringFromBundle(extras, Notification.EXTRA_TITLE);
            content = getStringFromBundle(extras, Notification.EXTRA_BIG_TEXT);
            if (TextUtils.isEmpty(content)) {
                content = getStringFromBundle(extras, Notification.EXTRA_TEXT);
            }
            if (TextUtils.isEmpty(content)) {
                content = getStringFromBundle(extras, Notification.EXTRA_INFO_TEXT);
            }
            if (TextUtils.isEmpty(content)) {
                content = getStringFromBundle(extras, Notification.EXTRA_SUB_TEXT);
            }
            
            // Try text lines for inbox style
            CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (textLines != null && textLines.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (CharSequence line : textLines) {
                    sb.append(line).append(" ");
                }
                if (TextUtils.isEmpty(content)) {
                    content = sb.toString().trim();
                }
            }
            
            // Log all extras for debugging
            Log.d(TAG, "=== Notification Extras ===");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                if (value instanceof CharSequence || value instanceof String) {
                    Log.d(TAG, key + " = " + value);
                }
            }
        }
        
        // Method 2: Try tickerText
        if (TextUtils.isEmpty(content) && notification.tickerText != null) {
            content = notification.tickerText.toString();
            Log.d(TAG, "Using tickerText: " + content);
        }
        
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Content: " + content);

        // Parse the notification
        UpiNotificationParser.ParsedNotification parsed = 
                UpiNotificationParser.parse(packageName, title, content);

        if (parsed != null && parsed.amount > 0) {
            saveTransaction(parsed, packageName);
        } else {
            Log.d(TAG, "Could not parse transaction from notification");
            ActivityLogger.logRejected(getApplicationContext(), "notification",
                    "Could not parse notification: " + (title + " " + content).substring(0, Math.min(50, (title + " " + content).length())),
                    "Parse failed", null, null);
        }
    }
    
    private String getStringFromBundle(Bundle bundle, String key) {
        CharSequence cs = bundle.getCharSequence(key);
        return cs != null ? cs.toString() : "";
    }

    private boolean isPaymentApp(String packageName) {
        return GPAY.equals(packageName)
                || PHONEPE.equals(packageName)
                || PAYTM.equals(packageName)
                || AMAZON_PAY.equals(packageName)
                || BHIM.equals(packageName)
                || CRED.equals(packageName)
                || WHATSAPP.equals(packageName);
    }

    private void saveTransaction(UpiNotificationParser.ParsedNotification parsed, String packageName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                RupexDatabase db = RupexDatabase.getInstance(getApplicationContext());

                long now = System.currentTimeMillis();
                long twoMinutesAgo = now - 120000; // 2 minutes window for cross-source dedup
                String type = parsed.isIncome ? "income" : "expense";
                
                // Check 1: Exact duplicate (same amount, merchant, within 2 minutes)
                PendingTransaction existing = db.pendingTransactionDao()
                        .findDuplicate(parsed.amount, parsed.merchant, twoMinutesAgo, now);
                
                if (existing != null) {
                    Log.d(TAG, "Duplicate transaction (same merchant), skipping");
                    ActivityLogger.logRejected(getApplicationContext(), "notification",
                            "Duplicate transaction detected",
                            "Same merchant and amount within 2 minutes",
                            parsed.amount, parsed.merchant);
                    return;
                }
                
                // Check 2: Cross-source duplicate (SMS might have different merchant name)
                // e.g., SMS says "UPI/DR" but notification says "KISHORE SENTHIL"
                PendingTransaction crossSource = db.pendingTransactionDao()
                        .findDuplicateByAmountAndTime(parsed.amount, type, twoMinutesAgo, now);
                
                if (crossSource != null) {
                    Log.d(TAG, "Cross-source duplicate detected (SMS already captured this). Amount: ₹" 
                            + parsed.amount + ", existing merchant: " + crossSource.getMerchant());
                    
                    // If notification has better merchant info, update the existing transaction
                    String existingMerchant = crossSource.getMerchant();
                    if ((existingMerchant == null || existingMerchant.isEmpty() 
                            || existingMerchant.contains("UPI") || existingMerchant.contains("IMPS"))
                            && parsed.merchant != null && !parsed.merchant.isEmpty()) {
                        // Update with better merchant name from notification
                        db.pendingTransactionDao().updateMerchant(crossSource.getId(), parsed.merchant);
                        Log.d(TAG, "Updated merchant name to: " + parsed.merchant);
                        ActivityLogger.logAdded(getApplicationContext(), "notification",
                                "Updated merchant info for existing transaction",
                                parsed.amount, parsed.merchant);
                    } else {
                        ActivityLogger.logRejected(getApplicationContext(), "notification",
                                "Cross-source duplicate",
                                "SMS already captured this transaction",
                                parsed.amount, parsed.merchant);
                    }
                    return;
                }

                // Create new transaction
                PendingTransaction txn = new PendingTransaction();
                txn.setAmount(parsed.amount);
                txn.setType(type);
                txn.setMerchant(parsed.merchant);
                txn.setCategory(parsed.category);
                txn.setBankName(getAppName(packageName));
                txn.setTransactionAt(now);
                txn.setCreatedAt(now);  // Important: set createdAt to prevent premature deletion
                txn.setSynced(false);
                txn.setSource("notification");  // Mark source as notification
                // Generate unique hash for UPI notification
                String hash = "UPI_" + now + "_" + parsed.amount + "_" + parsed.merchant.hashCode();
                txn.setSmsHash(hash);

                db.pendingTransactionDao().insert(txn);
                Log.d(TAG, "Saved UPI transaction: ₹" + parsed.amount + " to " + parsed.merchant);
                
                // Log successful addition
                ActivityLogger.logAdded(getApplicationContext(), "notification",
                        "Transaction added from " + getAppName(packageName),
                        parsed.amount, parsed.merchant);

            } catch (Exception e) {
                Log.e(TAG, "Error saving transaction", e);
            }
        });
    }

    private String getAppName(String packageName) {
        switch (packageName) {
            case GPAY: return "Google Pay";
            case PHONEPE: return "PhonePe";
            case PAYTM: return "Paytm";
            case AMAZON_PAY: return "Amazon Pay";
            case BHIM: return "BHIM";
            case CRED: return "CRED";
            case WHATSAPP: return "WhatsApp Pay";
            default: return "UPI";
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Not needed
    }
}
