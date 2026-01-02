package com.rupex.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rupex.app.data.local.entity.Transaction;

import java.util.List;

/**
 * DAO for synced transactions
 */
@Dao
public interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Transaction> transactions);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY transaction_at DESC")
    LiveData<List<Transaction>> getAllLive();

    @Query("SELECT * FROM transactions ORDER BY transaction_at DESC LIMIT :limit OFFSET :offset")
    List<Transaction> getPaginated(int limit, int offset);

    @Query("SELECT * FROM transactions WHERE id = :id")
    Transaction getById(String id);

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY transaction_at DESC")
    LiveData<List<Transaction>> getByTypeLive(String type);

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY transaction_at DESC")
    LiveData<List<Transaction>> getByAccountLive(String accountId);

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY transaction_at DESC")
    LiveData<List<Transaction>> getByCategoryLive(String categoryId);

    @Query("SELECT * FROM transactions WHERE transaction_at BETWEEN :startDate AND :endDate ORDER BY transaction_at DESC")
    List<Transaction> getByDateRange(long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income' AND transaction_at BETWEEN :startDate AND :endDate")
    Double getTotalIncome(long startDate, long endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense' AND transaction_at BETWEEN :startDate AND :endDate")
    Double getTotalExpense(long startDate, long endDate);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM transactions")
    int getCount();
}
