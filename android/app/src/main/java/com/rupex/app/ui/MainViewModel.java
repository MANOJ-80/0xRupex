package com.rupex.app.ui;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.rupex.app.data.local.RupexDatabase;
import com.rupex.app.data.local.dao.PendingTransactionDao;
import com.rupex.app.data.local.entity.PendingTransaction;
import com.rupex.app.data.model.Account;
import com.rupex.app.data.model.Category;
import com.rupex.app.data.model.CategoryStat;
import com.rupex.app.data.model.MonthlySummary;
import com.rupex.app.data.remote.ApiClient;
import com.rupex.app.data.remote.RupexApi;
import com.rupex.app.data.remote.model.ApiResponse;
import com.rupex.app.data.remote.model.PaginatedApiResponse;
import com.rupex.app.data.remote.model.AccountDto;
import com.rupex.app.data.remote.model.CategoryDto;
import com.rupex.app.data.remote.model.CreateTransactionRequest;
import com.rupex.app.data.remote.model.UpdateTransactionRequest;
import com.rupex.app.data.remote.model.TransactionDto;
import com.rupex.app.sync.SyncManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for MainActivity and Fragments
 */
public class MainViewModel extends AndroidViewModel {

    private static final String TAG = "MainViewModel";
    
    private final RupexDatabase database;
    private final RupexApi api;
    
    // Local data
    private final LiveData<List<PendingTransaction>> transactions;
    private final LiveData<Integer> pendingCount;
    private final LiveData<Integer> notificationParsedCount;
    private final LiveData<Double> totalBalance;
    private final LiveData<Double> totalIncome;
    private final LiveData<Double> totalExpense;
    private final MutableLiveData<String> syncStatus;
    
    // Remote data
    private final MutableLiveData<MonthlySummary> monthlySummary;
    private final MutableLiveData<List<CategoryStat>> categoryStats;
    private final MutableLiveData<List<Category>> categories;
    private final MutableLiveData<List<Account>> accounts;
    
    private String transactionFilter = "all";

    public MainViewModel(@NonNull Application application) {
        super(application);
        
        database = RupexDatabase.getInstance(application);
        api = ApiClient.getInstance(application).getApi();
        
        // Local LiveData
        transactions = database.pendingTransactionDao().getAllLive();
        pendingCount = database.pendingTransactionDao().getUnsyncedCountLive();
        notificationParsedCount = database.pendingTransactionDao().getNotificationParsedCountLive();
        // Calculate balance from transactions (income - expense)
        totalBalance = database.pendingTransactionDao().getNetBalanceLive();
        totalIncome = database.pendingTransactionDao().getTotalIncomeLive();
        totalExpense = database.pendingTransactionDao().getTotalExpenseLive();
        syncStatus = new MutableLiveData<>("Ready");
        
        // Remote LiveData
        monthlySummary = new MutableLiveData<>();
        categoryStats = new MutableLiveData<>();
        categories = new MutableLiveData<>();
        accounts = new MutableLiveData<>();
    }

    // ==================== TRANSACTIONS ====================
    
    public LiveData<List<PendingTransaction>> getTransactions() {
        return transactions;
    }

    public LiveData<Integer> getPendingCount() {
        return pendingCount;
    }

    public LiveData<Integer> getNotificationParsedCount() {
        return notificationParsedCount;
    }

    public LiveData<Double> getTotalBalance() {
        return totalBalance;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }

    public LiveData<String> getSyncStatus() {
        return syncStatus;
    }
    
    public void setTransactionFilter(String filter) {
        this.transactionFilter = filter;
        // In a real implementation, we'd filter the transactions
    }

    public void loadTransactions() {
        updateSyncStatus();
        // Fetch transactions from backend and store locally
        fetchTransactionsFromBackend();
    }
    
    /**
     * Fetch transactions from backend API and store in local database
     */
    private void fetchTransactionsFromBackend() {
        api.getTransactions(1, 100, null, null, null).enqueue(new Callback<PaginatedApiResponse<TransactionDto>>() {
            @Override
            public void onResponse(Call<PaginatedApiResponse<TransactionDto>> call,
                                  Response<PaginatedApiResponse<TransactionDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<TransactionDto> serverTransactions = response.body().getData();
                    if (serverTransactions != null && !serverTransactions.isEmpty()) {
                        Log.d(TAG, "Fetched " + serverTransactions.size() + " transactions from backend");
                        storeTransactionsLocally(serverTransactions);
                    } else {
                        Log.d(TAG, "No transactions from backend");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch transactions: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<PaginatedApiResponse<TransactionDto>> call, Throwable t) {
                Log.e(TAG, "Error fetching transactions: " + t.getMessage());
            }
        });
    }
    
    /**
     * Store fetched transactions in local database (avoiding duplicates)
     */
    private void storeTransactionsLocally(List<TransactionDto> serverTransactions) {
        Executors.newSingleThreadExecutor().execute(() -> {
            for (TransactionDto dto : serverTransactions) {
                try {
                    // Use server ID as hash to prevent duplicates
                    String hash = "SERVER_" + dto.getId();
                    
                    // Check 1: Already exists with this server hash
                    if (database.pendingTransactionDao().existsBySmsHash(hash)) {
                        continue; // Skip duplicate
                    }
                    
                    // Check 2: Check if there's a local transaction (manual/sms/notification) 
                    // that matches this server transaction - update it instead of creating new
                    long txnTime = parseServerDate(dto.getTransactionAt());
                    long fiveMinutes = 5 * 60 * 1000;
                    PendingTransaction existing = database.pendingTransactionDao()
                            .findDuplicateByAmountAndTime(dto.getAmount(), dto.getType(), 
                                    txnTime - fiveMinutes, txnTime + fiveMinutes);
                    
                    if (existing != null) {
                        // Update existing local transaction with server info
                        database.pendingTransactionDao().updateServerInfo(existing.getId(), dto.getId(), hash);
                        Log.d(TAG, "Updated existing local txn with server ID: " + dto.getId());
                        continue;
                    }
                    
                    // Create new transaction
                    PendingTransaction txn = new PendingTransaction();
                    txn.setAmount(dto.getAmount());
                    txn.setType(dto.getType());
                    txn.setMerchant(dto.getMerchant() != null ? dto.getMerchant() : dto.getDescription());
                    txn.setCategory(dto.getCategoryName());
                    txn.setNote(dto.getDescription());
                    txn.setSynced(true); // Already synced since it came from server
                    txn.setSource(dto.getSource() != null ? dto.getSource() : "synced");
                    txn.setSmsHash(hash);
                    txn.setServerId(dto.getId()); // Store server ID for backend delete
                    
                    // Parse transaction date
                    txn.setTransactionAt(txnTime);
                    txn.setCreatedAt(txnTime);
                    
                    database.pendingTransactionDao().insert(txn);
                    Log.d(TAG, "Stored from backend: " + dto.getMerchant() + " ₹" + dto.getAmount());
                } catch (Exception e) {
                    Log.e(TAG, "Error storing transaction: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Parse ISO date string from server
     */
    private long parseServerDate(String dateStr) {
        if (dateStr == null) return System.currentTimeMillis();
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(dateStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    /**
     * Update a transaction's category, type, note, amount, merchant, and date/time
     */
    public void updateTransaction(long transactionId, String category, String type, String note, Double amount, String merchant, Long transactionAt) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Update local database
            database.pendingTransactionDao().updateCategory(transactionId, category);
            database.pendingTransactionDao().updateType(transactionId, type);
            if (note != null) {
                database.pendingTransactionDao().updateNote(transactionId, note);
            }
            if (amount != null && amount > 0) {
                database.pendingTransactionDao().updateAmount(transactionId, amount);
            }
            if (merchant != null && !merchant.isEmpty()) {
                database.pendingTransactionDao().updateMerchant(transactionId, merchant);
            }
            if (transactionAt != null && transactionAt > 0) {
                database.pendingTransactionDao().updateTransactionAt(transactionId, transactionAt);
            }
            
            // Get the transaction to check if it has a server ID
            PendingTransaction txn = database.pendingTransactionDao().getById(transactionId);
            if (txn == null) {
                Log.e(TAG, "Transaction not found for update: " + transactionId);
                return;
            }
            
            // Check if this transaction exists on the server
            String serverId = txn.getServerId();
            if (serverId == null || serverId.isEmpty()) {
                // Try to extract from smsHash if it starts with SERVER_
                String smsHash = txn.getSmsHash();
                if (smsHash != null && smsHash.startsWith("SERVER_")) {
                    serverId = smsHash.substring(7);
                }
            }
            
            if (serverId != null && !serverId.isEmpty()) {
                // Sync update to backend immediately
                Log.d(TAG, "Syncing update to backend for server ID: " + serverId);
                
                // Use UpdateTransactionRequest - only includes non-null, non-empty values
                UpdateTransactionRequest request = new UpdateTransactionRequest()
                        .setType(type)
                        .setCategoryName(category);
                
                if (note != null) {
                    request.setNotes(note);
                }
                if (amount != null && amount > 0) {
                    request.setAmount(amount);
                }
                if (merchant != null && !merchant.isEmpty()) {
                    request.setMerchant(merchant);
                }
                if (transactionAt != null && transactionAt > 0) {
                    request.setTransactionAt(transactionAt);
                }
                
                try {
                    Response<ApiResponse<TransactionDto>> response = 
                            api.updateTransaction(serverId, request).execute();
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // Mark as synced
                        database.pendingTransactionDao().markSynced(transactionId, serverId);
                        Log.d(TAG, "Backend update successful for transaction: " + serverId);
                    } else {
                        String error = response.body() != null ? response.body().getError() : "Unknown error";
                        Log.e(TAG, "Backend update failed: " + error);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing update to backend", e);
                }
            } else {
                Log.d(TAG, "Transaction has no server ID, will sync on next sync cycle");
            }
        });
    }

    /**
     * Update a transaction's category, type, and note (legacy method)
     */
    public void updateTransaction(long transactionId, String category, String type, String note) {
        updateTransaction(transactionId, category, type, note, null, null, null);
    }

    /**
     * Update a transaction's category and type (legacy method)
     */
    public void updateTransaction(long transactionId, String category, String type) {
        updateTransaction(transactionId, category, type, null, null, null, null);
    }

    public void syncTransactions() {
        syncStatus.setValue("Syncing...");
        SyncManager.scheduleSyncNow(getApplication());
        
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            updateSyncStatus();
        }, 2, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void updateSyncStatus() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String time = sdf.format(new Date());
        syncStatus.postValue("Last sync: " + time);
    }
    
    // ==================== MONTHLY SUMMARY ====================
    
    public LiveData<MonthlySummary> getMonthlySummary() {
        return monthlySummary;
    }
    
    public void loadMonthlySummary() {
        Calendar cal = Calendar.getInstance();
        loadMonthlySummary(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }
    
    public void loadMonthlySummary(int year, int month) {
        // Load from local database using synchronous queries
        Executors.newSingleThreadExecutor().execute(() -> {
            double income = database.pendingTransactionDao().getTotalIncomeSync();
            double expense = database.pendingTransactionDao().getTotalExpenseSync();
            
            MonthlySummary summary = new MonthlySummary(income, expense, 0);
            monthlySummary.postValue(summary);
        });
    }
    
    // ==================== CATEGORY STATS ====================
    
    public LiveData<List<CategoryStat>> getCategoryStats() {
        return categoryStats;
    }
    
    public void loadCategoryStats(int year, int month) {
        // Load from local database instead of API
        Executors.newSingleThreadExecutor().execute(() -> {
            List<PendingTransactionDao.CategoryStatResult> results = 
                    database.pendingTransactionDao().getCategoryStatsForExpenses();
            
            if (results == null || results.isEmpty()) {
                categoryStats.postValue(new ArrayList<>());
                return;
            }
            
            // Calculate total for percentage
            double total = 0;
            for (PendingTransactionDao.CategoryStatResult result : results) {
                total += result.total;
            }
            
            // Convert to CategoryStat with percentage
            List<CategoryStat> stats = new ArrayList<>();
            int id = 1;
            for (PendingTransactionDao.CategoryStatResult result : results) {
                double percentage = total > 0 ? (result.total / total) * 100 : 0;
                String categoryName = result.category != null ? result.category : "Uncategorized";
                stats.add(new CategoryStat(id++, categoryName, result.total, percentage));
            }
            
            categoryStats.postValue(stats);
        });
    }
    
    public void loadCategoryStatsForType(String type) {
        // Load from local database for specific type (expense or income)
        Executors.newSingleThreadExecutor().execute(() -> {
            List<PendingTransactionDao.CategoryStatResult> results;
            if ("income".equals(type)) {
                results = database.pendingTransactionDao().getCategoryStatsForIncome();
            } else {
                results = database.pendingTransactionDao().getCategoryStatsForExpenses();
            }
            
            if (results == null || results.isEmpty()) {
                categoryStats.postValue(new ArrayList<>());
                return;
            }
            
            // Calculate total for percentage
            double total = 0;
            for (PendingTransactionDao.CategoryStatResult result : results) {
                total += result.total;
            }
            
            // Convert to CategoryStat with percentage
            List<CategoryStat> stats = new ArrayList<>();
            int id = 1;
            for (PendingTransactionDao.CategoryStatResult result : results) {
                double percentage = total > 0 ? (result.total / total) * 100 : 0;
                String categoryName = result.category != null ? result.category : "Uncategorized";
                stats.add(new CategoryStat(id++, categoryName, result.total, percentage));
            }
            
            categoryStats.postValue(stats);
        });
    }
    
    public void loadCategoryStatsForType(String type, int year, int month) {
        // Load from local database for specific type (expense or income) filtered by month
        Executors.newSingleThreadExecutor().execute(() -> {
            String yearStr = String.valueOf(year);
            String monthStr = String.format("%02d", month); // Pad with leading zero
            
            List<PendingTransactionDao.CategoryStatResult> results;
            if ("income".equals(type)) {
                results = database.pendingTransactionDao().getCategoryStatsForIncomeByMonth(yearStr, monthStr);
            } else {
                results = database.pendingTransactionDao().getCategoryStatsForExpensesByMonth(yearStr, monthStr);
            }
            
            if (results == null || results.isEmpty()) {
                categoryStats.postValue(new ArrayList<>());
                return;
            }
            
            // Calculate total for percentage
            double total = 0;
            for (PendingTransactionDao.CategoryStatResult result : results) {
                total += result.total;
            }
            
            // Convert to CategoryStat with percentage
            List<CategoryStat> stats = new ArrayList<>();
            int id = 1;
            for (PendingTransactionDao.CategoryStatResult result : results) {
                double percentage = total > 0 ? (result.total / total) * 100 : 0;
                String categoryName = result.category != null ? result.category : "Uncategorized";
                stats.add(new CategoryStat(id++, categoryName, result.total, percentage));
            }
            
            categoryStats.postValue(stats);
        });
    }
    
    // ==================== CATEGORIES ====================
    
    public LiveData<List<Category>> getCategories() {
        return categories;
    }
    
    public void loadCategories() {
        api.getCategories().enqueue(new Callback<ApiResponse<List<CategoryDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<CategoryDto>>> call,
                                 Response<ApiResponse<List<CategoryDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<CategoryDto> dtos = response.body().data;
                    List<Category> cats = new ArrayList<>();
                    for (CategoryDto dto : dtos) {
                        cats.add(new Category(dto.id, dto.name, dto.type, dto.icon, dto.color));
                    }
                    categories.postValue(cats);
                } else {
                    // Use default categories if API fails
                    categories.postValue(getDefaultCategories());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<CategoryDto>>> call, Throwable t) {
                // Use default categories on network failure
                categories.postValue(getDefaultCategories());
            }
        });
    }
    
    /**
     * Returns default categories when API is unavailable
     */
    private List<Category> getDefaultCategories() {
        List<Category> defaults = new ArrayList<>();
        // Expense categories
        defaults.add(new Category(1, "Food & Dining", "expense", "🍕", "#FF5722"));
        defaults.add(new Category(2, "Shopping", "expense", "🛍️", "#E91E63"));
        defaults.add(new Category(3, "Transportation", "expense", "🚗", "#2196F3"));
        defaults.add(new Category(4, "Bills & Utilities", "expense", "📄", "#FF9800"));
        defaults.add(new Category(5, "Entertainment", "expense", "🎬", "#9C27B0"));
        defaults.add(new Category(6, "Health", "expense", "💊", "#00BCD4"));
        defaults.add(new Category(7, "Education", "expense", "📚", "#3F51B5"));
        defaults.add(new Category(8, "Groceries", "expense", "🛒", "#4CAF50"));
        defaults.add(new Category(9, "Personal Care", "expense", "💅", "#F44336"));
        defaults.add(new Category(10, "Travel", "expense", "✈️", "#009688"));
        defaults.add(new Category(11, "Other", "expense", "📦", "#607D8B"));
        // Income categories
        defaults.add(new Category(12, "Salary", "income", "💰", "#4CAF50"));
        defaults.add(new Category(13, "Freelance", "income", "💼", "#8BC34A"));
        defaults.add(new Category(14, "Investment", "income", "📈", "#03A9F4"));
        defaults.add(new Category(15, "Refund", "income", "↩️", "#00BCD4"));
        defaults.add(new Category(16, "Gift", "income", "🎁", "#E91E63"));
        defaults.add(new Category(17, "Other Income", "income", "💵", "#607D8B"));
        return defaults;
    }
    
    // ==================== ACCOUNTS ====================
    
    public LiveData<List<Account>> getAccounts() {
        return accounts;
    }
    
    public void loadAccounts() {
        api.getAccounts().enqueue(new Callback<ApiResponse<List<AccountDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<AccountDto>>> call,
                                 Response<ApiResponse<List<AccountDto>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<AccountDto> dtos = response.body().data;
                    List<Account> accs = new ArrayList<>();
                    for (AccountDto dto : dtos) {
                        accs.add(new Account(dto.id, dto.name, dto.bankName, 
                                dto.accountNumber, dto.accountType, dto.balance));
                    }
                    if (accs.isEmpty()) {
                        accs = getDefaultAccounts();
                    }
                    accounts.postValue(accs);
                } else {
                    accounts.postValue(getDefaultAccounts());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<AccountDto>>> call, Throwable t) {
                accounts.postValue(getDefaultAccounts());
            }
        });
    }
    
    /**
     * Returns default accounts when API is unavailable
     */
    private List<Account> getDefaultAccounts() {
        List<Account> defaults = new ArrayList<>();
        defaults.add(new Account(1, "Cash", "Cash", "0000", "cash", 0.0));
        defaults.add(new Account(2, "Bank Account", "Bank", "XXXX", "savings", 0.0));
        defaults.add(new Account(3, "UPI", "UPI", "XXXX", "upi", 0.0));
        return defaults;
    }
    
    // ==================== CREATE TRANSACTION ====================
    
    public void createTransaction(double amount, String type, String description,
                                  String merchant, int categoryId, Integer accountId,
                                  Date date, String notes) {
        
        Log.d(TAG, "createTransaction called: amount=" + amount + ", type=" + type + ", desc=" + description);
        
        // Save to local database first (so it appears immediately)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PendingTransaction txn = new PendingTransaction();
                txn.setAmount(amount);
                txn.setType(type);
                txn.setMerchant(merchant != null && !merchant.isEmpty() ? merchant : description);
                txn.setCategory(getCategoryNameById(categoryId));
                txn.setNote(notes);
                txn.setTransactionAt(date.getTime());
                txn.setCreatedAt(System.currentTimeMillis());
                txn.setSynced(false);
                txn.setSource("manual");
                // Generate unique hash for manual transaction
                String hash = "MANUAL_" + System.currentTimeMillis() + "_" + amount + "_" + description.hashCode();
                txn.setSmsHash(hash);
                
                Log.d(TAG, "Inserting transaction: " + txn.getMerchant() + " amount=" + txn.getAmount());
                database.pendingTransactionDao().insert(txn);
                Log.d(TAG, "Transaction inserted successfully!");
            } catch (Exception e) {
                Log.e(TAG, "Error inserting transaction: " + e.getMessage(), e);
            }
        });
        
        // Also try to sync to backend
        // Use categoryName instead of categoryId for backend auto-resolution
        String categoryName = getCategoryNameById(categoryId);
        CreateTransactionRequest request = new CreateTransactionRequest()
                .setAmount(amount)
                .setType(type)
                .setDescription(description)
                .setMerchant(merchant)
                .setCategoryName(categoryName)
                .setAccountId(accountId != null ? String.valueOf(accountId) : null)
                .setTransactionAt(date.getTime())
                .setSource("manual");
        
        api.createTransaction(request).enqueue(new Callback<ApiResponse<TransactionDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<TransactionDto>> call,
                                 Response<ApiResponse<TransactionDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    TransactionDto serverTxn = response.body().getData();
                    // Update local transaction with server ID to prevent duplicate on next fetch
                    Executors.newSingleThreadExecutor().execute(() -> {
                        // Find the local transaction we just created (by amount, merchant, recent time)
                        long recentTime = System.currentTimeMillis() - 60000; // within last minute
                        PendingTransaction local = database.pendingTransactionDao()
                                .findDuplicateByAmountAndTime(amount, type, recentTime, System.currentTimeMillis());
                        if (local != null) {
                            // Update with server ID so it won't be re-fetched
                            String serverHash = "SERVER_" + serverTxn.getId();
                            database.pendingTransactionDao().updateServerInfo(local.getId(), serverTxn.getId(), serverHash);
                            Log.d(TAG, "Updated local transaction with server ID: " + serverTxn.getId());
                        }
                    });
                    loadMonthlySummary();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<TransactionDto>> call, Throwable t) {
                // Transaction is already saved locally, will sync later
                Log.e(TAG, "Backend save failed: " + t.getMessage());
            }
        });
    }
    
    /**
     * Get category name by ID from default categories
     */
    private String getCategoryNameById(int categoryId) {
        List<Category> cats = categories.getValue();
        if (cats != null) {
            for (Category cat : cats) {
                if (cat.getId() == categoryId) {
                    return cat.getName();
                }
            }
        }
        return "Other";
    }
    
    // ==================== DELETE TRANSACTION ====================
    
    /**
     * Delete a transaction from local database and backend
     */
    public void deleteTransaction(PendingTransaction transaction) {
        Log.d(TAG, "deleteTransaction called for ID: " + transaction.getId() + ", merchant: " + transaction.getMerchant());
        Executors.newSingleThreadExecutor().execute(() -> {
            // Delete from local database first
            database.pendingTransactionDao().deleteById(transaction.getId());
            Log.d(TAG, "Deleted from local DB: " + transaction.getId());
            
            // Get server ID from either serverId field or extract from smsHash
            String serverId = transaction.getServerId();
            if ((serverId == null || serverId.isEmpty()) && transaction.getSmsHash() != null 
                    && transaction.getSmsHash().startsWith("SERVER_")) {
                serverId = transaction.getSmsHash().substring(7); // Remove "SERVER_" prefix
            }
            if (serverId != null && !serverId.isEmpty()) {
                Log.d(TAG, "Deleting from backend with serverId: " + serverId);
                final String finalServerId = serverId;
                api.deleteTransaction(serverId).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                         Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Backend delete successful for: " + finalServerId);
                        } else {
                            Log.e(TAG, "Backend delete failed: " + response.code());
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Log.e(TAG, "Backend delete error: " + t.getMessage());
                    }
                });
            } else {
                Log.d(TAG, "No server ID, local-only delete");
            }
        });
    }
    
    /**
     * Delete transaction by ID
     */
    public void deleteTransactionById(long transactionId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            PendingTransaction txn = database.pendingTransactionDao().getById(transactionId);
            if (txn != null) {
                deleteTransaction(txn);
            }
        });
    }
    
    /**
     * Get transaction by ID (for undo functionality)
     */
    public PendingTransaction getTransactionById(long id) {
        return database.pendingTransactionDao().getById(id);
    }
}
