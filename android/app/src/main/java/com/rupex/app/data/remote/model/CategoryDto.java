package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * Category DTO from server
 */
public class CategoryDto {
    
    @SerializedName("id")
    public int id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("type")
    public String type;
    
    @SerializedName("icon")
    public String icon;
    
    @SerializedName("color")
    public String color;
    
    @SerializedName("is_system")
    public boolean isSystem;
}
