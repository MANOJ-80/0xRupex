package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class CategoryDto {

    @SerializedName("id")
    public String id;

    @SerializedName("userId")
    public String userId;

    @SerializedName("name")
    public String name;

    @SerializedName("type")
    public String type;

    @SerializedName("icon")
    public String icon;

    @SerializedName("color")
    public String color;

    @SerializedName("parentId")
    public String parentId;

    @SerializedName("isSystem")
    public boolean isSystem;

    @SerializedName("createdAt")
    public String createdAt;
}
