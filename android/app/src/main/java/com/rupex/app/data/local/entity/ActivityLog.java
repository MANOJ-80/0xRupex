package com.rupex.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Activity log entity - tracks what the app captured, added, or rejected
 */
@Entity(
    tableName = "activity_logs",
    indices = {
        @Index(value = "timestamp")
    }
)
public class ActivityLog {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "type")
    private String type; // "captured", "added", "rejected"

    @ColumnInfo(name = "source")
    private String source; // "sms", "notification", "manual"

    @ColumnInfo(name = "message")
    private String message; // Detailed message

    @ColumnInfo(name = "amount")
    private Double amount; // Transaction amount if applicable

    @ColumnInfo(name = "merchant")
    private String merchant; // Merchant name if applicable

    @ColumnInfo(name = "reason")
    private String reason; // Reason for rejection if applicable

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

