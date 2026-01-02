package com.rupex.app.ui;

import android.app.Application;

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
import com.rupex.app.data.remote.model.AccountDto;
import com.rupex.app.data.remote.model.CategoryDto;
import com.rupex.app.data.remote.model.CreateTransactionRequest;
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
    }

    /**
     * Update a transaction's category, type, and note
     */
    public void updateTransaction(long transactionId, String category, String type, String note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            database.pendingTransactionDao().updateCategory(transactionId, category);
            database.pendingTransactionDao().updateType(transactionId, type);
            if (note != null) {
                database.pendingTransactionDao().updateNote(transactionId, note);
            }
        });
    }

    /**
     * Update a transaction's category and type (legacy method)
     */
    public void updateTransaction(long transactionId, String category, String type) {
        updateTransaction(transactionId, category, type, null);
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
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<CategoryDto>>> call, Throwable t) {
                categories.postValue(new ArrayList<>());
            }
        });
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
                    accounts.postValue(accs);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<AccountDto>>> call, Throwable t) {
                accounts.postValue(new ArrayList<>());
            }
        });
    }
    
    // ==================== CREATE TRANSACTION ====================
    
    public void createTransaction(double amount, String type, String description,
                                  String merchant, int categoryId, Integer accountId,
                                  Date date, String notes) {
        
        CreateTransactionRequest request = new CreateTransactionRequest()
                .setAmount(amount)
                .setType(type)
                .setDescription(description)
                .setMerchant(merchant)
                .setCategoryId(String.valueOf(categoryId))
                .setAccountId(accountId != null ? String.valueOf(accountId) : null)
                .setTransactionAt(date.getTime())
                .setSource("manual");
        
        api.createTransaction(request).enqueue(new Callback<ApiResponse<TransactionDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<TransactionDto>> call,
                                 Response<ApiResponse<TransactionDto>> response) {
                if (response.isSuccessful()) {
                    // Refresh transactions
                    loadMonthlySummary();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<TransactionDto>> call, Throwable t) {
                // Handle error
            }
        });
    }
}
