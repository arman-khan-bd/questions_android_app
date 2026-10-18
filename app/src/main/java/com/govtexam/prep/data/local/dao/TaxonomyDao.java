package com.govtexam.prep.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.govtexam.prep.data.model.TaxonomyItem;

import java.util.List;

/**
 * Data Access Object for Subjects, Topics, Categories, and Tags taxonomy tree.
 */
@Dao
public interface TaxonomyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceAll(List<TaxonomyItem> items);

    @Query("SELECT * FROM taxonomies WHERE type = 'subject' ORDER BY title ASC")
    LiveData<List<TaxonomyItem>> getSubjects();

    @Query("SELECT * FROM taxonomies WHERE type = 'topic' ORDER BY questionsCount DESC")
    LiveData<List<TaxonomyItem>> getTopics();

    @Query("SELECT * FROM taxonomies WHERE type = 'category' ORDER BY title ASC")
    LiveData<List<TaxonomyItem>> getCategories();

    @Query("SELECT * FROM taxonomies WHERE type = 'tag' ORDER BY title ASC")
    LiveData<List<TaxonomyItem>> getTags();

    @Query("SELECT * FROM taxonomies WHERE type = 'subject' ORDER BY title ASC")
    List<TaxonomyItem> getSubjectsSync();

    @Query("SELECT * FROM taxonomies WHERE type = 'topic' ORDER BY questionsCount DESC")
    List<TaxonomyItem> getTopicsSync();

    @Query("SELECT * FROM taxonomies WHERE type = 'category' ORDER BY title ASC")
    List<TaxonomyItem> getCategoriesSync();

    @Query("SELECT * FROM taxonomies WHERE type = 'tag' ORDER BY title ASC")
    List<TaxonomyItem> getTagsSync();

    @Query("DELETE FROM taxonomies WHERE type = :type")
    void clearTaxonomyByType(String type);
}
