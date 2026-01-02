package com.rupex.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Transaction entity - Synced transactions from server
 */
@Entity(
    tableName = "transactions",
    indices = {
        @Index(value = "account_id"),
        @Index(value = "category_id"),
        @Index(value = "transaction_at")
    }
)
public class Transaction {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id; // Server UUID

    @ColumnInfo(name = "account_id")
    private String accountId;

    @ColumnInfo(name = "category_id")
    private String categoryId;

    @NonNull
    @ColumnInfo(name = "type")
    private String type; // income, expense, transfer

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "merchant")
    private String merchant;

    @ColumnInfo(name = "reference_id")
    private String referenceId;

    @ColumnInfo(name = "source")
    private String source; // manual, sms, api

    @ColumnInfo(name = "transaction_at")
    private long transactionAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    // Joined fields (not persisted, set manually)
    private transient String accountName;
    private transient String categoryName;
    private transient String categoryColor;

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    @NonNull
    public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public long getTransactionAt() { return transactionAt; }
    public void setTransactionAt(long transactionAt) { this.transactionAt = transactionAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryColor() { return categoryColor; }
    public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }
}
