package com.questions.govt.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.questions.govt.data.local.entity.NotificationEntity;

import java.util.List;

/**
 * Data Access Object for Notifications with strict anti-duplication enforcement.
 */
@Dao
public interface NotificationDao {

    /**
     * Inserts a single notification.
     * Uses OnConflictStrategy.IGNORE so duplicate events on the composite unique index are safely ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(NotificationEntity notification);

    /**
     * Inserts or replaces a list of notifications fetched from the server.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceAll(List<NotificationEntity> notifications);

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    LiveData<List<NotificationEntity>> getAllNotifications();

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    LiveData<Integer> getUnreadCount();

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    void markAsRead(long id);

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    void markAllAsRead();

    @Query("DELETE FROM notifications")
    void clearAll();
}
