package com.rupex.app.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.rupex.app.data.local.RupexDatabase;
import com.rupex.app.data.local.entity.PendingTransaction;
import com.rupex.app.sms.parser.SmsParser;
import com.rupex.app.sms.parser.ParsedSms;
import com.rupex.app.sync.SyncManager;
import com.rupex.app.util.ActivityLogger;

import java.util.concurrent.Executors;

/**
 * BroadcastReceiver for intercepting incoming SMS messages.
 * Filters for bank SMS and parses transaction data.
 * 
 * PRIVACY NOTE: We only extract structured data (amount, type, reference).
 * Raw SMS content is NEVER stored.
 */
public class SmsBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        // Extract SMS messages from intent
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;

        // Combine multi-part SMS
        StringBuilder fullMessage = new StringBuilder();
        String sender = null;
        long timestamp = System.currentTimeMillis();

        for (SmsMessage sms : messages) {
            if (sms != null) {
                fullMessage.append(sms.getMessageBody());
                if (sender == null) {
                    sender = sms.getOriginatingAddress();
                    timestamp = sms.getTimestampMillis();
                }
            }
        }

        String smsBody = fullMessage.toString();
        String finalSender = sender != null ? sender : "UNKNOWN";

        Log.d(TAG, "SMS received from: " + finalSender);

        // Check if this is a bank SMS worth parsing
        if (!SmsParser.isBankSms(finalSender)) {
            Log.d(TAG, "Not a bank SMS, ignoring");
            return;
        }

        // Process in background to avoid ANR
        long finalTimestamp = timestamp;
        Executors.newSingleThreadExecutor().execute(() -> {
            processBankSms(context, finalSender, smsBody, finalTimestamp);
        });
    }

    /**
     * Process bank SMS and create pending transaction
     */
    private void processBankSms(Context context, String sender, String smsBody, long timestamp) {
        try {
            // Parse the SMS
            ParsedSms parsed = SmsParser.parse(sender, smsBody, timestamp);

            if (parsed == null || !parsed.isValid()) {
                Log.d(TAG, "Could not parse SMS or invalid data");
                return;
            }

            Log.i(TAG, String.format("Parsed transaction: %s ₹%.2f from %s | Merchant: %s | Category: %s",
                    parsed.getType(), parsed.getAmount(), parsed.getBankName(),
                    parsed.getMerchant(), parsed.getCategory()));

            // Log that SMS was captured
            ActivityLogger.logCaptured(context, "sms",
                    "SMS from " + parsed.getBankName(),
                    parsed.getAmount(), parsed.getMerchant());

            // Create pending transaction entity
            PendingTransaction pendingTxn = new PendingTransaction();
            pendingTxn.setType(parsed.getType());
            pendingTxn.setAmount(parsed.getAmount());
            pendingTxn.setLast4Digits(parsed.getLast4Digits());
            pendingTxn.setReferenceId(parsed.getReferenceId());
            pendingTxn.setMerchant(parsed.getMerchant());
            pendingTxn.setBalance(parsed.getBalance());
            pendingTxn.setBankName(parsed.getBankName());
            pendingTxn.setCategory(parsed.getCategory());
            pendingTxn.setCategoryIcon(parsed.getCategoryIcon());
            pendingTxn.setCategoryColor(parsed.getCategoryColor());
            pendingTxn.setSmsHash(parsed.getSmsHash());
            pendingTxn.setTransactionAt(timestamp);
            pendingTxn.setCreatedAt(System.currentTimeMillis());
            pendingTxn.setSynced(false);
            pendingTxn.setSource("sms");  // Mark source as SMS

            // Save to local database
            RupexDatabase db = RupexDatabase.getInstance(context);
            
            // Check 1: Duplicate by SMS hash (exact same SMS)
            if (db.pendingTransactionDao().existsBySmsHash(pendingTxn.getSmsHash())) {
                Log.d(TAG, "Duplicate SMS detected, skipping");
                ActivityLogger.logRejected(context, "sms",
                        "Duplicate SMS detected",
                        "Same SMS hash already exists",
                        parsed.getAmount(), parsed.getMerchant());
                return;
            }
            
            // Check 2: Cross-source duplicate (notification might have already captured this)
            // Use 15-minute window (900000ms) because SMS can be delayed significantly
            long timeWindow = 900000; 
            long startTime = timestamp - timeWindow;
            long endTime = timestamp + timeWindow;
            
            PendingTransaction crossSource = db.pendingTransactionDao()
                    .findDuplicateLoose(parsed.getAmount(), parsed.getType(), startTime, endTime);
            
            if (crossSource != null) {
                String existingMerchant = crossSource.getMerchant();
                String newMerchant = parsed.getMerchant();
                
                Log.d(TAG, "Potential cross-source duplicate. Amount: ₹" + parsed.getAmount()
                        + ", existing merchant: " + existingMerchant 
                        + ", new merchant: " + newMerchant);
                
                // Determine if this is truly a duplicate or two different transactions
                boolean isGenericMerchant = existingMerchant == null || existingMerchant.isEmpty()
                        || existingMerchant.contains("UPI") || existingMerchant.contains("IMPS")
                        || existingMerchant.contains("DR/") || existingMerchant.contains("CR/");
                
                boolean existingHasGenericMerchant = isGenericMerchant;
                boolean newHasGenericMerchant = newMerchant == null || newMerchant.isEmpty()
                        || newMerchant.contains("UPI") || newMerchant.contains("IMPS")
                        || newMerchant.contains("DR/") || newMerchant.contains("CR/");
                        
                boolean merchantsAreSimilar = areMerchantsSimilar(existingMerchant, newMerchant);
                
                // Only consider it a duplicate if:
                // 1. One or both have generic merchant (e.g., "UPI-REF" vs "John Doe")
                // 2. OR merchant names are similar enough to be the same person
                if (existingHasGenericMerchant || newHasGenericMerchant || merchantsAreSimilar) {
                    // SMS usually has better bank info, so update the existing transaction
                    if (parsed.getBankName() != null && !parsed.getBankName().isEmpty()) {
                        // Update bank name and account info from SMS
                        db.pendingTransactionDao().updateBankInfo(crossSource.getId(), 
                                parsed.getBankName(), parsed.getLast4Digits());
                        Log.d(TAG, "Updated bank info to: " + parsed.getBankName());
                        ActivityLogger.logAdded(context, "sms",
                                "Updated bank info for existing transaction",
                                parsed.getAmount(), parsed.getMerchant());
                    } else {
                        ActivityLogger.logRejected(context, "sms",
                                "Cross-source duplicate",
                                "Notification already captured this transaction",
                                parsed.getAmount(), parsed.getMerchant());
                    }
                    return;
                } else {
                    // Different merchant names = different transactions, proceed to add
                    Log.d(TAG, "Different merchants detected, treating as separate transaction. " +
                            "Existing: '" + existingMerchant + "' vs New: '" + newMerchant + "'");
                }
            }

            // Insert into database
            long id = db.pendingTransactionDao().insert(pendingTxn);
            Log.i(TAG, "Saved pending transaction with ID: " + id);

            // Log successful addition
            ActivityLogger.logAdded(context, "sms",
                    "Transaction added from SMS",
                    parsed.getAmount(), parsed.getMerchant());

            // Trigger sync
            SyncManager.scheduleSyncNow(context);

        } catch (Exception e) {
            Log.e(TAG, "Error processing bank SMS", e);
        }
    }
    
    /**
     * Check if two merchant names are similar enough to be considered the same person/entity.
     */
    private boolean areMerchantsSimilar(String merchant1, String merchant2) {
        if (merchant1 == null || merchant2 == null) {
            return false;
        }
        
        String norm1 = normalizeMerchant(merchant1);
        String norm2 = normalizeMerchant(merchant2);
        
        if (norm1.isEmpty() || norm2.isEmpty()) {
            return false;
        }
        
        // Exact match after normalization
        if (norm1.equals(norm2)) {
            return true;
        }
        
        // Check if one contains the other
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            return true;
        }
        
        // Check first word match
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
                .replaceAll("^(MR\\s*|MRS\\s*|MS\\s*|DR\\s*)", "")
                .replaceAll("[^A-Z0-9\\s]", "")
                .trim();
    }
}
