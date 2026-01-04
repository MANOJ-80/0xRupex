package com.rupex.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.rupex.app.data.local.entity.ActivityLog;

import java.util.List;

/**
 * DAO for activity logs
 */
@Dao
public interface ActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ActivityLog log);

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<ActivityLog>> getRecentLogs(int limit);

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    List<ActivityLog> getRecentLogsSync(int limit);

    @Query("DELETE FROM activity_logs WHERE timestamp < :beforeTimestamp")
    void deleteOldLogs(long beforeTimestamp);

    @Query("DELETE FROM activity_logs")
    void deleteAll();
}

