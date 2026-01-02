package com.rupex.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.rupex.app.data.local.entity.Account;

import java.util.List;

/**
 * DAO for accounts
 */
@Dao
public interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Account> accounts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Account account);

    @Query("SELECT * FROM accounts WHERE is_active = 1 ORDER BY name")
    LiveData<List<Account>> getActiveLive();

    @Query("SELECT * FROM accounts ORDER BY name")
    List<Account> getAll();

    @Query("SELECT * FROM accounts WHERE id = :id")
    Account getById(String id);

    @Query("SELECT * FROM accounts WHERE last_4_digits = :last4Digits AND is_active = 1 LIMIT 1")
    Account getByLast4Digits(String last4Digits);

    @Query("SELECT SUM(balance) FROM accounts WHERE is_active = 1")
    LiveData<Double> getTotalBalanceLive();

    @Query("UPDATE accounts SET balance = :balance, updated_at = :timestamp WHERE id = :id")
    void updateBalance(String id, double balance, long timestamp);

    @Query("DELETE FROM accounts")
    void deleteAll();
}
