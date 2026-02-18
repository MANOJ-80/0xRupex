package com.rupex.app.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rupex.app.data.local.RupexDatabase;
import com.rupex.app.data.local.entity.PendingTransaction;
import com.rupex.app.data.remote.ApiClient;
import com.rupex.app.data.remote.RupexApi;
import com.rupex.app.data.remote.model.ApiResponse;
import com.rupex.app.data.remote.model.CreateTransactionRequest;
import com.rupex.app.data.remote.model.UpdateTransactionRequest;
import com.rupex.app.data.remote.model.TransactionDto;
import com.rupex.app.util.TokenManager;

import java.util.List;

import retrofit2.Response;

/**
 * Background worker to sync pending transactions with server
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Starting sync work");

        Context context = getApplicationContext();
        TokenManager tokenManager = TokenManager.getInstance(context);

        // Check if user is logged in
        if (!tokenManager.isLoggedIn()) {
            Log.w(TAG, "User not logged in, skipping sync");
            return Result.success();
        }

        RupexDatabase db = RupexDatabase.getInstance(context);
        RupexApi api = ApiClient.getInstance(context).getApi();

        // Get unsynced transactions
        List<PendingTransaction> unsynced = db.pendingTransactionDao().getUnsynced();
        
        if (unsynced.isEmpty()) {
            Log.i(TAG, "No pending transactions to sync");
            return Result.success();
        }

        Log.i(TAG, "Syncing " + unsynced.size() + " pending transactions");

        int successCount = 0;
        int failCount = 0;

        for (PendingTransaction pending : unsynced) {
            try {
                // Determine source - default to "sms" if not set
                String source = pending.getSource();
                if (source == null || source.isEmpty()) {
                    source = "sms";
                }
                
                // Build description - use note if available, otherwise construct from merchant
                String description = pending.getNote();
                if (description == null || description.isEmpty()) {
                    if (pending.getMerchant() != null && !pending.getMerchant().isEmpty()) {
                        description = "Payment to " + pending.getMerchant();
                    } else {
                        description = pending.getType().equals("income") ? "Income" : "Expense";
                    }
                }
                
                // Build request
                CreateTransactionRequest request = new CreateTransactionRequest()
                        .setType(pending.getType())
                        .setAmount(pending.getAmount())
                        .setMerchant(pending.getMerchant())
                        .setDescription(description)
                        .setReferenceId(pending.getReferenceId())
                        .setTransactionAt(pending.getTransactionAt())
                        .setSmsHash(pending.getSmsHash())
                        .setCategoryName(pending.getCategory())
                        .setLast4Digits(pending.getLast4Digits())
                        .setNotes(pending.getNote())
                        .setSource(source);

                // Try to match account by last 4 digits
                if (pending.getLast4Digits() != null) {
                    var account = db.accountDao().getByLast4Digits(pending.getLast4Digits());
                    if (account != null) {
                        request.setAccountId(account.getId());
                    }
                }

                // Make API call
                Response<ApiResponse<TransactionDto>> response = 
                        api.createTransaction(request).execute();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TransactionDto created = response.body().getData();
                    
                    // Mark as synced
                    db.pendingTransactionDao().markSynced(pending.getId(), created.getId());
                    successCount++;
                    
                    Log.d(TAG, "Synced transaction: " + created.getId());
                } else {
                    String error = response.body() != null ? response.body().getError() : "Unknown error";
                    db.pendingTransactionDao().setSyncError(pending.getId(), error);
                    failCount++;
                    
                    Log.w(TAG, "Failed to sync transaction: " + error);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error syncing transaction " + pending.getId(), e);
                db.pendingTransactionDao().setSyncError(pending.getId(), e.getMessage());
                failCount++;
            }
        }

        Log.i(TAG, String.format("Sync complete: %d success, %d failed", successCount, failCount));

        // Now sync any updated transactions (have server_id but synced = 0)
        syncUpdatedTransactions(db, api);

        // Clean up old synced transactions (older than 30 days)
        // Only delete if created_at is a valid timestamp (> year 2020)
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L);
        long minValidTimestamp = 1577836800000L; // Jan 1, 2020
        if (thirtyDaysAgo > minValidTimestamp) {
            db.pendingTransactionDao().deleteOldSynced(thirtyDaysAgo);
        }

        return failCount > 0 ? Result.retry() : Result.success();
    }

    /**
     * Sync transactions that have been updated locally but not yet synced to server
     */
    private void syncUpdatedTransactions(RupexDatabase db, RupexApi api) {
        List<PendingTransaction> updated = db.pendingTransactionDao().getUpdatedNotSynced();
        
        if (updated.isEmpty()) {
            return;
        }

        Log.i(TAG, "Syncing " + updated.size() + " updated transactions");

        for (PendingTransaction pending : updated) {
            try {
                // Use UpdateTransactionRequest - only includes non-null, non-empty values
                UpdateTransactionRequest request = new UpdateTransactionRequest()
                        .setType(pending.getType())
                        .setCategoryName(pending.getCategory());
                
                // Include notes if available
                if (pending.getNote() != null && !pending.getNote().isEmpty()) {
                    request.setNotes(pending.getNote());
                }

                Response<ApiResponse<TransactionDto>> response = 
                        api.updateTransaction(pending.getServerId(), request).execute();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Mark as synced again
                    db.pendingTransactionDao().markSynced(pending.getId(), pending.getServerId());
                    Log.d(TAG, "Updated transaction on server: " + pending.getServerId());
                } else {
                    String error = response.body() != null ? response.body().getError() : "Unknown error";
                    Log.w(TAG, "Failed to update transaction: " + error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating transaction " + pending.getId(), e);
            }
        }
    }
}
