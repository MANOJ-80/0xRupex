package com.rupex.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.rupex.app.data.local.entity.Category;

import java.util.List;

/**
 * DAO for categories
 */
@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Category> categories);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);

    @Query("SELECT * FROM categories ORDER BY is_system DESC, name")
    LiveData<List<Category>> getAllLive();

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY is_system DESC, name")
    LiveData<List<Category>> getByTypeLive(String type);

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY is_system DESC, name")
    List<Category> getByType(String type);

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getById(String id);

    @Query("SELECT * FROM categories WHERE name LIKE :name LIMIT 1")
    Category getByName(String name);

    @Query("DELETE FROM categories")
    void deleteAll();
}
