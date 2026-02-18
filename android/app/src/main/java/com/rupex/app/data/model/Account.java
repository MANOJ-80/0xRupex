package com.rupex.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Account {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("institution")
    private String institution;

    @SerializedName("type")
    private String type;

    @SerializedName("accountNumber")
    private String accountNumber;

    @SerializedName("last4Digits")
    private String last4Digits;

    @SerializedName("balance")
    private double balance;

    @SerializedName("color")
    private String color;

    @SerializedName("icon")
    private String icon;

    @SerializedName("isActive")
    private boolean isActive;

    public Account() {}

    public Account(String id, String name, String institution, String accountNumber,
                   String type, double balance) {
        this.id = id;
        this.name = name;
        this.institution = institution;
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getInstitution() { return institution; }
    public String getBankName() { return institution; }
    public String getAccountNumber() { return accountNumber; }
    public String getLast4Digits() { return last4Digits; }
    public String getType() { return type; }
    public String getAccountType() { return type; }
    public double getBalance() { return balance; }
    public String getColor() { return color; }
    public String getIcon() { return icon; }
    public boolean isActive() { return isActive; }

    public String getDisplayName() {
        if (last4Digits != null && !last4Digits.isEmpty()) {
            return name + " (**" + last4Digits + ")";
        }
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
