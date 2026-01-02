package com.rupex.app.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.rupex.app.data.local.dao.AccountDao;
import com.rupex.app.data.local.dao.CategoryDao;
import com.rupex.app.data.local.dao.PendingTransactionDao;
import com.rupex.app.data.local.dao.TransactionDao;
import com.rupex.app.data.local.entity.Account;
import com.rupex.app.data.local.entity.Category;
import com.rupex.app.data.local.entity.PendingTransaction;
import com.rupex.app.data.local.entity.Transaction;

/**
 * Room Database for 0xRupex
 * 
 * Stores:
 * - Pending transactions (from SMS, awaiting sync)
 * - Synced transactions (from server)
 * - Accounts
 * - Categories
 */
@Database(
    entities = {
        PendingTransaction.class,
        Transaction.class,
        Account.class,
        Category.class
    },
    version = 3,
    exportSchema = false
)
public abstract class RupexDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "rupex_db";
    private static volatile RupexDatabase INSTANCE;

    // DAOs
    public abstract PendingTransactionDao pendingTransactionDao();
    public abstract TransactionDao transactionDao();
    public abstract AccountDao accountDao();
    public abstract CategoryDao categoryDao();

    /**
     * Get singleton database instance
     */
    public static RupexDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RupexDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            RupexDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Clear all tables (for logout)
     */
    public void clearAllData() {
        if (INSTANCE != null) {
            INSTANCE.runInTransaction(() -> {
                INSTANCE.pendingTransactionDao().deleteOldSynced(Long.MAX_VALUE);
                INSTANCE.transactionDao().deleteAll();
                INSTANCE.accountDao().deleteAll();
                INSTANCE.categoryDao().deleteAll();
            });
        }
    }
}
