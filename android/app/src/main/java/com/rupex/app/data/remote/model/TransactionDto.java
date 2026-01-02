package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Transaction DTO from server
 */
public class TransactionDto {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("accountId")
    private String accountId;
    
    @SerializedName("categoryId")
    private String categoryId;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("amount")
    private double amount;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("merchant")
    private String merchant;
    
    @SerializedName("referenceId")
    private String referenceId;
    
    @SerializedName("source")
    private String source;
    
    @SerializedName("transactionAt")
    private String transactionAt;
    
    @SerializedName("createdAt")
    private String createdAt;

    // Getters
    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getCategoryId() { return categoryId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getMerchant() { return merchant; }
    public String getReferenceId() { return referenceId; }
    public String getSource() { return source; }
    public String getTransactionAt() { return transactionAt; }
    public String getCreatedAt() { return createdAt; }

    /**
     * Paginated response wrapper
     */
    public static class PaginatedResponse {
        @SerializedName("transactions")
        private List<TransactionDto> transactions;
        
        @SerializedName("pagination")
        private Pagination pagination;

        public List<TransactionDto> getTransactions() { return transactions; }
        public Pagination getPagination() { return pagination; }
    }

    public static class Pagination {
        @SerializedName("page")
        private int page;
        
        @SerializedName("limit")
        private int limit;
        
        @SerializedName("total")
        private int total;
        
        @SerializedName("totalPages")
        private int totalPages;

        public int getPage() { return page; }
        public int getLimit() { return limit; }
        public int getTotal() { return total; }
        public int getTotalPages() { return totalPages; }
    }
}
