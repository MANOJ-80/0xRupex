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

    @Query("DELETE FROM pending_transactions WHERE synced = 1 AND created_at < :beforeTimestamp")
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

    // Category stats for expenses
    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM pending_transactions WHERE type = 'expense' GROUP BY category ORDER BY total DESC")
    List<CategoryStatResult> getCategoryStatsForExpenses();

    // Category stats for income
    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM pending_transactions WHERE type = 'income' GROUP BY category ORDER BY total DESC")
    List<CategoryStatResult> getCategoryStatsForIncome();

    // Inner class for category stats result
    class CategoryStatResult {
        public String category;
        public double total;
        public int count;
    }
}
