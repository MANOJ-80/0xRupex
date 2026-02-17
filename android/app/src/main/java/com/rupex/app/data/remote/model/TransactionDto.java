package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TransactionDto {

    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

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

    @SerializedName("location")
    private String location;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("notes")
    private String notes;

    @SerializedName("isRecurring")
    private boolean isRecurring;

    @SerializedName("smsHash")
    private String smsHash;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("categoryIcon")
    private String categoryIcon;

    @SerializedName("categoryColor")
    private String categoryColor;

    @SerializedName("accountName")
    private String accountName;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getAccountId() { return accountId; }
    public String getCategoryId() { return categoryId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getMerchant() { return merchant; }
    public String getReferenceId() { return referenceId; }
    public String getSource() { return source; }
    public String getTransactionAt() { return transactionAt; }
    public String getLocation() { return location; }
    public List<String> getTags() { return tags; }
    public String getNotes() { return notes; }
    public boolean isRecurring() { return isRecurring; }
    public String getSmsHash() { return smsHash; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryIcon() { return categoryIcon; }
    public String getCategoryColor() { return categoryColor; }
    public String getAccountName() { return accountName; }
}
