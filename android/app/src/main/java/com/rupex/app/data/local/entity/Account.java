package com.rupex.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Account entity - Bank accounts, wallets, cash
 */
@Entity(tableName = "accounts")
public class Account {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id; // Server UUID

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @NonNull
    @ColumnInfo(name = "type")
    private String type; // bank, wallet, cash, credit_card

    @ColumnInfo(name = "institution")
    private String institution;

    @ColumnInfo(name = "balance")
    private double balance;

    @ColumnInfo(name = "last_4_digits")
    private String last4Digits;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "icon")
    private String icon;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @NonNull
    public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getLast4Digits() { return last4Digits; }
    public void setLast4Digits(String last4Digits) { this.last4Digits = last4Digits; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
