package com.rupex.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Category statistics model
 */
public class CategoryStat {
    
    @SerializedName("category_id")
    private int categoryId;
    
    @SerializedName("category_name")
    private String categoryName;
    
    @SerializedName("icon")
    private String icon;
    
    @SerializedName("color")
    private String color;
    
    @SerializedName("total")
    private double total;
    
    @SerializedName("percentage")
    private double percentage;
    
    @SerializedName("count")
    private int count;
    
    public CategoryStat() {}
    
    public CategoryStat(int categoryId, String categoryName, double total, double percentage) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.total = total;
        this.percentage = percentage;
    }

    // Getters
    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public double getTotal() { return total; }
    public double getPercentage() { return percentage; }
    public int getCount() { return count; }
}
