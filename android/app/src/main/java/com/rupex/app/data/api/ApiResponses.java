package com.rupex.app.data.api;

import com.google.gson.annotations.SerializedName;
import com.rupex.app.data.model.Transaction;
import com.rupex.app.data.model.Account;
import com.rupex.app.data.model.Category;
import com.rupex.app.data.model.CategoryStat;
import com.rupex.app.data.model.MonthlySummary;

import java.util.List;

/**
 * API Response wrapper classes
 */
public class ApiResponses {

    /**
     * Generic wrapper for API responses
     */
    public static class BaseResponse<T> {
        @SerializedName("success")
        private boolean success;
        
        @SerializedName("message")
        private String message;
        
        @SerializedName("data")
        private T data;
        
        @SerializedName("error")
        private String error;
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
        public String getError() { return error; }
    }
    
    /**
     * Pagination info
     */
    public static class Pagination {
        @SerializedName("page")
        private int page;
        
        @SerializedName("limit")
        private int limit;
        
        @SerializedName("total")
        private int total;
        
        @SerializedName("total_pages")
        private int totalPages;
        
        public int getPage() { return page; }
        public int getLimit() { return limit; }
        public int getTotal() { return total; }
        public int getTotalPages() { return totalPages; }
    }
    
    /**
     * Transactions list response
     */
    public static class TransactionsResponse {
        @SerializedName("success")
        private boolean success;
        
        @SerializedName("data")
        private List<Transaction> data;
        
        @SerializedName("pagination")
        private Pagination pagination;
        
        public boolean isSuccess() { return success; }
        public List<Transaction> getData() { return data; }
        public Pagination getPagination() { return pagination; }
    }
    
    /**
     * Accounts response
     */
    public static class AccountsData {
        @SerializedName("accounts")
        private List<Account> accounts;
        
        @SerializedName("total_balance")
        private double totalBalance;
        
        public List<Account> getAccounts() { return accounts; }
        public double getTotalBalance() { return totalBalance; }
    }
    
    /**
     * Categories response
     */
    public static class CategoriesData {
        @SerializedName("categories")
        private List<Category> categories;
        
        public List<Category> getCategories() { return categories; }
    }
    
    /**
     * Category stats response
     */
    public static class CategoryStatsData {
        @SerializedName("stats")
        private List<CategoryStat> stats;
        
        public List<CategoryStat> getStats() { return stats; }
    }
    
    /**
     * Monthly summary response
     */
    public static class SummaryData {
        @SerializedName("summary")
        private MonthlySummary summary;
        
        public MonthlySummary getSummary() { return summary; }
    }
    
    /**
     * Single transaction response
     */
    public static class TransactionData {
        @SerializedName("transaction")
        private Transaction transaction;
        
        public Transaction getTransaction() { return transaction; }
    }
}
