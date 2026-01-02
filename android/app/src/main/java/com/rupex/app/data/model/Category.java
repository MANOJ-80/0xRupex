package com.rupex.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Category model for API responses
 */
public class Category {
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("icon")
    private String icon;
    
    @SerializedName("color")
    private String color;
    
    @SerializedName("is_system")
    private boolean isSystem;
    
    public Category() {}
    
    public Category(int id, String name, String type, String icon, String color) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public boolean isSystem() { return isSystem; }
    
    public boolean isExpenseCategory() {
        return "expense".equals(type);
    }
    
    public boolean isIncomeCategory() {
        return "income".equals(type);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
