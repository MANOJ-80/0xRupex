package com.rupex.app.data.remote;

import com.rupex.app.data.remote.model.*;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

/**
 * Retrofit API interface for 0xRupex backend
 */
public interface RupexApi {

    // ============================================
    // AUTHENTICATION
    // ============================================
    
    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);
    
    @POST("auth/register")
    Call<ApiResponse<AuthResponse>> register(@Body LoginRequest request);
    
    @POST("auth/refresh")
    Call<ApiResponse<AuthResponse>> refreshToken(@Body RefreshTokenRequest request);
    
    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();

    // ============================================
    // ACCOUNTS
    // ============================================
    
    @GET("accounts")
    Call<ApiResponse<List<AccountDto>>> getAccounts();
    
    @POST("accounts")
    Call<ApiResponse<AccountDto>> createAccount(@Body AccountDto account);
    
    @GET("accounts/{id}")
    Call<ApiResponse<AccountDto>> getAccount(@Path("id") String id);

    // ============================================
    // TRANSACTIONS
    // ============================================
    
    @GET("transactions")
    Call<ApiResponse<TransactionDto.PaginatedResponse>> getTransactions(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("type") String type,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );
    
    @POST("transactions")
    Call<ApiResponse<TransactionDto>> createTransaction(@Body CreateTransactionRequest request);
    
    @POST("transactions/bulk")
    Call<ApiResponse<List<TransactionDto>>> createTransactionsBulk(
            @Body List<CreateTransactionRequest> requests
    );
    
    @GET("transactions/{id}")
    Call<ApiResponse<TransactionDto>> getTransaction(@Path("id") String id);
    
    @PUT("transactions/{id}")
    Call<ApiResponse<TransactionDto>> updateTransaction(
            @Path("id") String id,
            @Body CreateTransactionRequest request
    );
    
    @DELETE("transactions/{id}")
    Call<ApiResponse<Void>> deleteTransaction(@Path("id") String id);

    // ============================================
    // CATEGORIES
    // ============================================
    
    @GET("categories")
    Call<ApiResponse<List<CategoryDto>>> getCategories();
    
    @GET("categories")
    Call<ApiResponse<List<CategoryDto>>> getCategoriesByType(@Query("type") String type);
    
    @GET("categories/stats")
    Call<ApiResponse<CategoryStatsResponse>> getCategoryStats(
            @Query("start_date") String startDate,
            @Query("end_date") String endDate
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
            @Query("start_date") String startDate,
            @Query("end_date") String endDate
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
    
    class SummaryResponse {
        public MonthlySummaryDto summary;
    }
    
    class CategoryStatsResponse {
        public java.util.List<CategoryStatDto> stats;
    }
    
    class AnalyticsResponse {
        public java.util.List<DailyTrend> daily_trend;
        public java.util.List<TopMerchant> top_merchants;
    }
    
    class MonthlySummaryDto {
        public int year;
        public int month;
        public double total_income;
        public double total_expense;
        public double net_savings;
        public int transaction_count;
        
        // Convenience getters
        public double getIncome() { return total_income; }
        public double getExpenses() { return total_expense; }
        public int getTransactionCount() { return transaction_count; }
    }
    
    class CategoryStatDto {
        public int categoryId;
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
