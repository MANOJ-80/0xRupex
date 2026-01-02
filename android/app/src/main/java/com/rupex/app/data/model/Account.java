package com.rupex.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Account model for API responses
 */
public class Account {
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("bank_name")
    private String bankName;
    
    @SerializedName("account_number")
    private String accountNumber;
    
    @SerializedName("account_type")
    private String accountType;
    
    @SerializedName("balance")
    private double balance;
    
    @SerializedName("is_active")
    private boolean isActive;
    
    public Account() {}
    
    public Account(int id, String name, String bankName, String accountNumber, 
                   String accountType, double balance) {
        this.id = id;
        this.name = name;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getBankName() { return bankName; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountType() { return accountType; }
    public double getBalance() { return balance; }
    public boolean isActive() { return isActive; }
    
    public String getDisplayName() {
        if (accountNumber != null && accountNumber.length() >= 4) {
            String last4 = accountNumber.substring(accountNumber.length() - 4);
            return name + " (**" + last4 + ")";
        }
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
