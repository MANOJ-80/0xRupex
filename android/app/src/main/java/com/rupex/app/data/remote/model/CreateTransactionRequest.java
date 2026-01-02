package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * Create transaction request (for syncing parsed SMS)
 */
public class CreateTransactionRequest {
    
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
    
    @SerializedName("smsHash")
    private String smsHash;
    
    @SerializedName("categoryName")
    private String categoryName;  // For auto-matching to backend category
    
    @SerializedName("last4Digits")
    private String last4Digits;   // For account matching
    
    @SerializedName("notes")
    private String notes;  // User notes for the transaction

    public CreateTransactionRequest() {
        this.source = "sms";
    }

    // Builder pattern for cleaner construction
    public CreateTransactionRequest setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public CreateTransactionRequest setCategoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public CreateTransactionRequest setType(String type) {
        this.type = type;
        return this;
    }

    public CreateTransactionRequest setAmount(double amount) {
        this.amount = amount;
        return this;
    }

    public CreateTransactionRequest setDescription(String description) {
        this.description = description;
        return this;
    }

    public CreateTransactionRequest setMerchant(String merchant) {
        this.merchant = merchant;
        return this;
    }

    public CreateTransactionRequest setReferenceId(String referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    public CreateTransactionRequest setSource(String source) {
        this.source = source;
        return this;
    }

    public CreateTransactionRequest setTransactionAt(long timestamp) {
        this.transactionAt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                java.util.Locale.US).format(new java.util.Date(timestamp));
        return this;
    }

    public CreateTransactionRequest setSmsHash(String smsHash) {
        this.smsHash = smsHash;
        return this;
    }
    
    public CreateTransactionRequest setCategoryName(String categoryName) {
        this.categoryName = categoryName;
        return this;
    }
    
    public CreateTransactionRequest setLast4Digits(String last4Digits) {
        this.last4Digits = last4Digits;
        return this;
    }
    
    public CreateTransactionRequest setNotes(String notes) {
        this.notes = notes;
        return this;
    }
}
