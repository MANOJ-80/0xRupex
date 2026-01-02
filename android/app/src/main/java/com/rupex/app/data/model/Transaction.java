package com.rupex.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;

/**
 * Transaction model for API responses
 */
public class Transaction {
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("account_id")
    private String accountId;
    
    @SerializedName("category_id")
    private String categoryId;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("amount")
    private double amount;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("merchant")
    private String merchant;
    
    @SerializedName("source")
    private String source;
    
    @SerializedName("transaction_at")
    private String transactionAt;
    
    @SerializedName("notes")
    private String notes;
    
    @SerializedName("is_recurring")
    private boolean isRecurring;
    
    @SerializedName("category_name")
    private String categoryName;
    
    @SerializedName("category_icon")
    private String categoryIcon;
    
    @SerializedName("category_color")
    private String categoryColor;
    
    @SerializedName("account_name")
    private String accountName;

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getAccountId() { return accountId; }
    public String getCategoryId() { return categoryId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getMerchant() { return merchant; }
    public String getSource() { return source; }
    public String getTransactionAt() { return transactionAt; }
    public String getNotes() { return notes; }
    public boolean isRecurring() { return isRecurring; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryIcon() { return categoryIcon; }
    public String getCategoryColor() { return categoryColor; }
    public String getAccountName() { return accountName; }
    
    // Setters for manual entry
    public void setType(String type) { this.type = type; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setDescription(String description) { this.description = description; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setTransactionAt(String transactionAt) { this.transactionAt = transactionAt; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public boolean isExpense() {
        return "expense".equals(type);
    }
    
    public boolean isIncome() {
        return "income".equals(type);
    }
    
    public String getDisplayTitle() {
        if (merchant != null && !merchant.isEmpty()) {
            return merchant;
        }
        if (description != null && !description.isEmpty()) {
            return description;
        }
        return categoryName != null ? categoryName : "Transaction";
    }
}
