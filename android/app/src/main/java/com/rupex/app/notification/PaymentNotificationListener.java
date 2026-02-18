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
                || CRED.equals(packageName);
    }

    private void saveTransaction(UpiNotificationParser.ParsedNotification parsed, String packageName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                RupexDatabase db = RupexDatabase.getInstance(getApplicationContext());

                long now = System.currentTimeMillis();
                long timeWindow = 900000; // 15 minutes for robustness
                long startTime = now - timeWindow;
                long endTime = now + timeWindow;

                String type = parsed.isIncome ? "income" : "expense";
                
                // Check 1: Exact duplicate (same amount, merchant, within window)
                PendingTransaction existing = db.pendingTransactionDao()
                        .findDuplicate(parsed.amount, parsed.merchant, startTime, endTime);
                
                if (existing != null) {
                    Log.d(TAG, "Duplicate transaction (same merchant), skipping");
                    ActivityLogger.logRejected(getApplicationContext(), "notification",
                            "Duplicate transaction detected",
                            "Same merchant and amount",
                            parsed.amount, parsed.merchant);
                    return;
                }
                
                // Check 2: Cross-source duplicate (SMS might have different merchant name)
                // e.g., SMS says "UPI/DR" but notification says "KISHORE SENTHIL"
                // BUT: Two DIFFERENT transactions with same amount at same time from DIFFERENT people
                // should NOT be considered duplicates!
                PendingTransaction crossSource = db.pendingTransactionDao()
                        .findDuplicateLoose(parsed.amount, type, startTime, endTime);
                
                if (crossSource != null) {
                    String existingMerchant = crossSource.getMerchant();
                    String existingSource = crossSource.getSource();
                    String newMerchant = parsed.merchant != null ? parsed.merchant.trim() : "";
                    
                    Log.d(TAG, "Potential cross-source duplicate. Amount: ₹" + parsed.amount 
                            + ", existing merchant: " + existingMerchant 
                            + ", new merchant: " + newMerchant
                            + ", existing source: " + existingSource);
                    
                    // Determine if this is truly a duplicate or two different transactions
                    boolean isGenericMerchant = existingMerchant == null || existingMerchant.isEmpty()
                            || existingMerchant.contains("UPI") || existingMerchant.contains("IMPS")
                            || existingMerchant.contains("DR/") || existingMerchant.contains("CR/");
                    
                    boolean merchantsAreSimilar = areMerchantsSimilar(existingMerchant, newMerchant);
                    
                    // Only consider it a duplicate if:
                    // 1. Existing merchant is generic (SMS with UPI ref) - notification has better info
                    // 2. OR merchant names are similar enough to be the same person
                    if (isGenericMerchant || merchantsAreSimilar) {
                        // This is a true cross-source duplicate
                        if (isGenericMerchant && !newMerchant.isEmpty()) {
                            // Update with better merchant name from notification
                            db.pendingTransactionDao().updateMerchant(crossSource.getId(), newMerchant);
                            Log.d(TAG, "Updated merchant name to: " + newMerchant);
                            ActivityLogger.logAdded(getApplicationContext(), "notification",
                                    "Updated merchant info for existing transaction",
                                    parsed.amount, newMerchant);
                        } else {
                            ActivityLogger.logRejected(getApplicationContext(), "notification",
                                    "Cross-source duplicate",
                                    "Already captured this transaction",
                                    parsed.amount, newMerchant);
                        }
                        return;
                    } else {
                        // Different merchant names = different transactions, proceed to add
                        Log.d(TAG, "Different merchants detected, treating as separate transaction. " +
                                "Existing: '" + existingMerchant + "' vs New: '" + newMerchant + "'");
                    }
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
                String merchantHash = parsed.merchant != null ? String.valueOf(parsed.merchant.hashCode()) : "unknown";
                String hash = "UPI_" + now + "_" + parsed.amount + "_" + merchantHash;
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
    
    /**
     * Check if two merchant names are similar enough to be considered the same person/entity.
     * This helps distinguish between:
     * - Same transaction from different sources (SMS: "UPI-REF123" vs Notification: "MANO RAJKUMAR") 
     * - Two DIFFERENT transactions from different people with same amount
     */
    private boolean areMerchantsSimilar(String merchant1, String merchant2) {
        if (merchant1 == null || merchant2 == null) {
            return false; // If either is null, can't compare - treat as different
        }
        
        // Normalize: trim, uppercase, remove common prefixes
        String norm1 = normalizeMerchant(merchant1);
        String norm2 = normalizeMerchant(merchant2);
        
        if (norm1.isEmpty() || norm2.isEmpty()) {
            return false; // Empty after normalization = can't compare
        }
        
        // Exact match after normalization
        if (norm1.equals(norm2)) {
            return true;
        }
        
        // Check if one contains the other (partial match)
        // e.g., "MANO RAJKUMAR" contains "MANO"
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            return true;
        }
        
        // Check first word match (often the first name)
        String[] words1 = norm1.split("\\s+");
        String[] words2 = norm2.split("\\s+");
        if (words1.length > 0 && words2.length > 0 && 
            words1[0].length() > 2 && words2[0].length() > 2 &&
            words1[0].equals(words2[0])) {
            return true;
        }
        
        return false;
    }
    
    private String normalizeMerchant(String merchant) {
        if (merchant == null) return "";
        return merchant.trim()
                .toUpperCase()
                .replaceAll("^(MR\\s*|MRS\\s*|MS\\s*|DR\\s*)", "") // Remove titles
                .replaceAll("[^A-Z0-9\\s]", "") // Remove special chars
                .trim();
    }

    private String getAppName(String packageName) {
        switch (packageName) {
            case GPAY: return "Google Pay";
            case PHONEPE: return "PhonePe";
            case PAYTM: return "Paytm";
            case AMAZON_PAY: return "Amazon Pay";
            case BHIM: return "BHIM";
            case CRED: return "CRED";
            default: return "UPI";
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Not needed
    }
}
