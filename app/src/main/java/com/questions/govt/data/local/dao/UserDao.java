package com.questions.govt.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.questions.govt.data.model.User;

/**
 * Data Access Object for User profile caching.
 */
@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(User user);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    LiveData<User> getUserById(long id);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserByIdSync(long id);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    LiveData<User> getUserByUsername(String username);

    @Query("DELETE FROM users")
    void clearUsers();
}
