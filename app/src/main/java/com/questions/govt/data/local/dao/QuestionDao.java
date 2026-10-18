package com.questions.govt.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.questions.govt.data.model.Question;

import java.util.List;

/**
 * Data Access Object for Questions, Starred Items, and Mistake Notebook.
 */
@Dao
public interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(Question question);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceAll(List<Question> questions);

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    LiveData<Question> getQuestionById(long id);

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    Question getQuestionByIdSync(long id);

    @Query("SELECT * FROM questions WHERE isStarred = 1 ORDER BY id DESC")
    LiveData<List<Question>> getStarredQuestions();

    @Query("SELECT * FROM questions WHERE isMistake = 1 ORDER BY id DESC")
    LiveData<List<Question>> getMistakeNotebookQuestions();

    @Query("UPDATE questions SET isStarred = :isStarred, starsCount = :starsCount WHERE id = :questionId")
    void updateStarStatus(long questionId, boolean isStarred, int starsCount);

    @Query("UPDATE questions SET isMistake = :isMistake WHERE id = :questionId")
    void updateMistakeStatus(long questionId, boolean isMistake);

    @Query("UPDATE questions SET viewsCount = :viewsCount, attemptsCount = :attemptsCount, " +
            "optionAPct = :optA, optionBPct = :optB, optionCPct = :optC, optionDPct = :optD WHERE id = :questionId")
    void updateTelemetry(long questionId, int viewsCount, int attemptsCount, float optA, float optB, float optC, float optD);

    @Query("SELECT * FROM questions WHERE questionText LIKE '%' || :query || '%' OR subjectTitle LIKE '%' || :query || '%' LIMIT 50")
    LiveData<List<Question>> searchQuestionsOffline(String query);

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId")
    LiveData<List<Question>> getQuestionsBySubject(long subjectId);

    @Query("SELECT * FROM questions WHERE topicId = :topicId")
    LiveData<List<Question>> getQuestionsByTopic(long topicId);

    @Query("DELETE FROM questions WHERE isStarred = 0 AND isMistake = 0")
    void clearNonPersistentCache();
}
