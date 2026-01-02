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

            // Save to local database
            RupexDatabase db = RupexDatabase.getInstance(context);
            
            // Check for duplicate (by SMS hash)
            if (db.pendingTransactionDao().existsBySmsHash(pendingTxn.getSmsHash())) {
                Log.d(TAG, "Duplicate SMS detected, skipping");
                return;
            }

            // Insert into database
            long id = db.pendingTransactionDao().insert(pendingTxn);
            Log.i(TAG, "Saved pending transaction with ID: " + id);

            // Trigger sync
            SyncManager.scheduleSyncNow(context);

        } catch (Exception e) {
            Log.e(TAG, "Error processing bank SMS", e);
        }
    }
}
