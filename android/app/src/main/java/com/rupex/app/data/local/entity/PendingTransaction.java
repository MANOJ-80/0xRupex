package com.rupex.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Pending transaction entity - transactions parsed from SMS awaiting sync
 */
@Entity(
    tableName = "pending_transactions",
    indices = {
        @Index(value = "sms_hash", unique = true),
        @Index(value = "synced")
    }
)
public class PendingTransaction {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "type")
    private String type; // "income" or "expense"

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "last_4_digits")
    private String last4Digits;

    @ColumnInfo(name = "reference_id")
    private String referenceId;

    @ColumnInfo(name = "merchant")
    private String merchant;

    @ColumnInfo(name = "balance")
    private Double balance;

    @ColumnInfo(name = "bank_name")
    private String bankName;

    @NonNull
    @ColumnInfo(name = "sms_hash")
    private String smsHash;

    @ColumnInfo(name = "transaction_at")
    private long transactionAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "synced")
    private boolean synced;

    @ColumnInfo(name = "server_id")
    private String serverId; // ID from backend after sync

    @ColumnInfo(name = "sync_error")
    private String syncError;
    
    @ColumnInfo(name = "category")
    private String category;  // Auto-detected category name
    
    @ColumnInfo(name = "category_icon")
    private String categoryIcon;
    
    @ColumnInfo(name = "category_color")
    private String categoryColor;
    
    @ColumnInfo(name = "note")
    private String note;  // User-added note like "Borrowed from Bob"
    
    @ColumnInfo(name = "source")
    private String source;  // "sms" or "notification" - where this transaction came from

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getLast4Digits() { return last4Digits; }
    public void setLast4Digits(String last4Digits) { this.last4Digits = last4Digits; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    @NonNull
    public String getSmsHash() { return smsHash; }
    public void setSmsHash(@NonNull String smsHash) { this.smsHash = smsHash; }

    public long getTransactionAt() { return transactionAt; }
    public void setTransactionAt(long transactionAt) { this.transactionAt = transactionAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getSyncError() { return syncError; }
    public void setSyncError(String syncError) { this.syncError = syncError; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
    
    public String getCategoryColor() { return categoryColor; }
    public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
