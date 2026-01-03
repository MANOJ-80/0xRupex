package com.rupex.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rupex.app.data.local.entity.PendingTransaction;

import java.util.List;

/**
 * DAO for pending transactions (parsed from SMS, awaiting sync)
 */
@Dao
public interface PendingTransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(PendingTransaction transaction);

    @Update
    void update(PendingTransaction transaction);

    @Query("SELECT * FROM pending_transactions ORDER BY transaction_at DESC")
    LiveData<List<PendingTransaction>> getAllLive();

    @Query("SELECT * FROM pending_transactions WHERE synced = 0 ORDER BY transaction_at ASC")
    List<PendingTransaction> getUnsynced();

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE synced = 0")
    int getUnsyncedCount();

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE synced = 0")
    LiveData<Integer> getUnsyncedCountLive();

    @Query("SELECT EXISTS(SELECT 1 FROM pending_transactions WHERE sms_hash = :smsHash)")
    boolean existsBySmsHash(String smsHash);

    @Query("UPDATE pending_transactions SET synced = 1, server_id = :serverId WHERE id = :id")
    void markSynced(long id, String serverId);

    @Query("UPDATE pending_transactions SET sync_error = :error WHERE id = :id")
    void setSyncError(long id, String error);

    // Delete old synced transactions - only those with valid created_at timestamps
    // The created_at check (> 1577836800000 = Jan 1 2020) prevents deleting transactions with default/zero timestamps
    @Query("DELETE FROM pending_transactions WHERE synced = 1 AND created_at > 1577836800000 AND created_at < :beforeTimestamp")
    void deleteOldSynced(long beforeTimestamp);

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) - COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) FROM pending_transactions")
    LiveData<Double> getNetBalanceLive();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM pending_transactions WHERE type = 'income'")
    LiveData<Double> getTotalIncomeLive();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM pending_transactions WHERE type = 'income'")
    double getTotalIncomeSync();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM pending_transactions WHERE type = 'expense'")
    LiveData<Double> getTotalExpenseLive();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM pending_transactions WHERE type = 'expense'")
    double getTotalExpenseSync();

    @Query("UPDATE pending_transactions SET category = :category, synced = 0 WHERE id = :id")
    void updateCategory(long id, String category);

    @Query("UPDATE pending_transactions SET type = :type, synced = 0 WHERE id = :id")
    void updateType(long id, String type);

    @Query("UPDATE pending_transactions SET note = :note, synced = 0 WHERE id = :id")
    void updateNote(long id, String note);

    @Query("SELECT * FROM pending_transactions WHERE synced = 0 AND server_id IS NOT NULL ORDER BY transaction_at ASC")
    List<PendingTransaction> getUpdatedNotSynced();

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    PendingTransaction getById(long id);

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    void deleteById(long id);

    // Find duplicate transaction (same amount, merchant, within time range)
    @Query("SELECT * FROM pending_transactions WHERE amount = :amount AND merchant = :merchant AND transaction_at BETWEEN :startTime AND :endTime LIMIT 1")
    PendingTransaction findDuplicate(double amount, String merchant, long startTime, long endTime);
    
    // Find duplicate by amount and time only (for cross-source deduplication: SMS vs Notification)
    // This catches cases where bank SMS and UPI notification both report same transaction
    @Query("SELECT * FROM pending_transactions WHERE amount = :amount AND type = :type AND transaction_at BETWEEN :startTime AND :endTime LIMIT 1")
    PendingTransaction findDuplicateByAmountAndTime(double amount, String type, long startTime, long endTime);
    
    // Update merchant name (used when notification has better info than SMS)
    @Query("UPDATE pending_transactions SET merchant = :merchant, synced = 0 WHERE id = :id")
    void updateMerchant(long id, String merchant);
    
    // Update amount
    @Query("UPDATE pending_transactions SET amount = :amount, synced = 0 WHERE id = :id")
    void updateAmount(long id, double amount);
    
    // Update transaction date/time
    @Query("UPDATE pending_transactions SET transaction_at = :transactionAt, synced = 0 WHERE id = :id")
    void updateTransactionAt(long id, long transactionAt);
    
    // Update bank info (used when SMS has better info than notification)
    @Query("UPDATE pending_transactions SET bank_name = :bankName, last_4_digits = :last4Digits, synced = 0 WHERE id = :id")
    void updateBankInfo(long id, String bankName, String last4Digits);
    
    // Update server info after successful backend save (prevents duplicate on fetch)
    @Query("UPDATE pending_transactions SET server_id = :serverId, sms_hash = :smsHash, synced = 1 WHERE id = :id")
    void updateServerInfo(long id, String serverId, String smsHash);

    // Category stats for expenses
    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM pending_transactions WHERE type = 'expense' GROUP BY category ORDER BY total DESC")
    List<CategoryStatResult> getCategoryStatsForExpenses();

    // Category stats for income
    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM pending_transactions WHERE type = 'income' GROUP BY category ORDER BY total DESC")
    List<CategoryStatResult> getCategoryStatsForIncome();
    
    // Category stats for expenses filtered by month
    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM pending_transactions WHERE type = 'expense' AND strftime('%Y', transaction_at/1000, 'unixepoch') = :year AND strftime('%m', transaction_at/1000, 'unixepoch') = :month GROUP BY category ORDER BY total DESC")
    List<CategoryStatResult> getCategoryStatsForExpensesByMonth(String year, String month);

    // Category stats for income filtered by month
    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM pending_transactions WHERE type = 'income' AND strftime('%Y', transaction_at/1000, 'unixepoch') = :year AND strftime('%m', transaction_at/1000, 'unixepoch') = :month GROUP BY category ORDER BY total DESC")
    List<CategoryStatResult> getCategoryStatsForIncomeByMonth(String year, String month);

    // Inner class for category stats result
    class CategoryStatResult {
        public String category;
        public double total;
        public int count;
    }
}
