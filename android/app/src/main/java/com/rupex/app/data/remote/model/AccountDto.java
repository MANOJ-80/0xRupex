package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * Account DTO from server
 */
public class AccountDto {
    
    @SerializedName("id")
    public int id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("bank_name")
    public String bankName;
    
    @SerializedName("account_number")
    public String accountNumber;
    
    @SerializedName("account_type")
    public String accountType;
    
    @SerializedName("balance")
    public double balance;
    
    @SerializedName("is_active")
    public boolean isActive;
}
