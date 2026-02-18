package com.rupex.app.data.remote;

import com.rupex.app.data.remote.model.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

public interface RupexApi {

    // ============================================
    // AUTHENTICATION
    // ============================================

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("auth/refresh")
    Call<AuthResponse> refreshToken(@Body RefreshTokenRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();

    // ============================================
    // ACCOUNTS
    // ============================================

    @GET("accounts")
    Call<AccountsResponse> getAccounts();

    @POST("accounts")
    Call<ApiResponse<AccountDto>> createAccount(@Body AccountDto account);

    @GET("accounts/{id}")
    Call<ApiResponse<AccountDto>> getAccount(@Path("id") String id);

    // ============================================
    // TRANSACTIONS
    // ============================================

    @GET("transactions")
    Call<PaginatedApiResponse<TransactionDto>> getTransactions(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("type") String type,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @POST("transactions")
    Call<ApiResponse<TransactionDto>> createTransaction(@Body CreateTransactionRequest request);

    @POST("transactions/sync")
    Call<ApiResponse<SyncResult>> syncTransactions(@Body SyncRequest request);

    @GET("transactions/{id}")
    Call<ApiResponse<TransactionDto>> getTransaction(@Path("id") String id);

    @PUT("transactions/{id}")
    Call<ApiResponse<TransactionDto>> updateTransaction(
            @Path("id") String id,
            @Body UpdateTransactionRequest request
    );

    @DELETE("transactions/{id}")
    Call<ApiResponse<Void>> deleteTransaction(@Path("id") String id);

    // ============================================
    // CATEGORIES
    // ============================================

    @GET("categories")
    Call<CategoriesResponse> getCategories(@Query("type") String type);

    @GET("categories/stats")
    Call<ApiResponse<CategoryStatsResponse>> getCategoryStats(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @POST("categories")
    Call<ApiResponse<CategoryDto>> createCategory(@Body CategoryDto category);

    // ============================================
    // ANALYTICS
    // ============================================

    @GET("transactions/summary")
    Call<ApiResponse<SummaryResponse>> getMonthlySummary(
            @Query("year") int year,
            @Query("month") int month
    );

    @GET("transactions/analytics")
    Call<ApiResponse<AnalyticsResponse>> getAnalytics(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    // ============================================
    // HELPER CLASSES
    // ============================================

    class RefreshTokenRequest {
        private String refreshToken;

        public RefreshTokenRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    class AccountsResponse {
        public boolean success;
        public List<AccountDto> accounts;
        public double totalBalance;
    }

    class CategoriesResponse {
        public boolean success;
        public List<CategoryDto> categories;
    }

    class SummaryResponse {
        public MonthlySummaryDto summary;
    }

    class CategoryStatsResponse {
        public java.util.List<CategoryStatDto> stats;
    }

    class AnalyticsResponse {
        public java.util.List<DailyTrend> dailyTrend;
        public java.util.List<TopMerchant> topMerchants;
    }

    class SyncRequest {
        public java.util.List<TransactionDto> transactions;
        public SyncRequest(java.util.List<TransactionDto> transactions) {
            this.transactions = transactions;
        }
    }

    class SyncResult {
        public int created;
        public int skipped;
        public java.util.List<SyncError> errors;
    }

    class SyncError {
        public TransactionDto transaction;
        public String error;
    }

    class MonthlySummaryDto {
        public int year;
        public int month;
        public double totalIncome;
        public double totalExpense;
        public double netSavings;
        public int transactionCount;

        public double getIncome() { return totalIncome; }
        public double getExpenses() { return totalExpense; }
        public int getTransactionCount() { return transactionCount; }
    }

    class CategoryStatDto {
        public String categoryId;
        public String categoryName;
        public String icon;
        public String color;
        public double total;
        public double percentage;
        public int count;
    }

    class DailyTrend {
        public String date;
        public double expense;
        public double income;
    }

    class TopMerchant {
        public String merchant;
        public double total;
        public int count;
    }
}
