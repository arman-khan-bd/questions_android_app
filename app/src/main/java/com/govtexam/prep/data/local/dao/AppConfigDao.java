package com.govtexam.prep.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.govtexam.prep.data.local.entity.AppConfigEntity;

/**
 * Data Access Object for AppConfigEntity.
 */
@Dao
public interface AppConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(AppConfigEntity config);

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    LiveData<AppConfigEntity> getAppConfig();

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    AppConfigEntity getAppConfigSync();
}
