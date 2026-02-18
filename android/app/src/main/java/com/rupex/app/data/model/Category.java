package com.rupex.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Category {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type;

    @SerializedName("icon")
    private String icon;

    @SerializedName("color")
    private String color;

    @SerializedName("isSystem")
    private boolean isSystem;

    public Category() {}

    public Category(String id, String name, String type, String icon, String color) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
    }

    public String getId() { return id; }
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
