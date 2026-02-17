package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class AccountDto {

    @SerializedName("id")
    public String id;

    @SerializedName("userId")
    public String userId;

    @SerializedName("name")
    public String name;

    @SerializedName("type")
    public String type;

    @SerializedName("institution")
    public String institution;

    @SerializedName("accountNumber")
    public String accountNumber;

    @SerializedName("last4Digits")
    public String last4Digits;

    @SerializedName("balance")
    public double balance;

    @SerializedName("color")
    public String color;

    @SerializedName("icon")
    public String icon;

    @SerializedName("isActive")
    public boolean isActive;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("updatedAt")
    public String updatedAt;
}
