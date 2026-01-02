package com.rupex.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Category entity - Transaction categories
 */
@Entity(tableName = "categories")
public class Category {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id; // Server UUID

    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    @NonNull
    @ColumnInfo(name = "type")
    private String type; // income, expense

    @ColumnInfo(name = "icon")
    private String icon;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "parent_id")
    private String parentId;

    @ColumnInfo(name = "is_system")
    private boolean isSystem;

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

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }
}
