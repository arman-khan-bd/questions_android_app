package com.govtexam.prep.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.govtexam.prep.data.local.entity.OfflineExamSubmission;
import com.govtexam.prep.data.model.ExamResult;

import java.util.List;

/**
 * Data Access Object for Exam Results and Offline Exam Submissions Queue.
 */
@Dao
public interface ExamDao {

    // Exam Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExamResult(ExamResult result);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExamResults(List<ExamResult> results);

    @Query("SELECT * FROM exam_results ORDER BY id DESC")
    LiveData<List<ExamResult>> getAllExamResults();

    @Query("SELECT * FROM exam_results WHERE id = :id LIMIT 1")
    LiveData<ExamResult> getExamResultById(long id);

    @Query("SELECT * FROM exam_results WHERE examId = :examId LIMIT 1")
    ExamResult getExamResultByExamIdSync(long examId);

    // Offline Submissions Queue
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOfflineSubmission(OfflineExamSubmission submission);

    @Query("SELECT * FROM offline_exam_submissions WHERE isSynced = 0 ORDER BY localId ASC")
    List<OfflineExamSubmission> getPendingSubmissions();

    @Query("UPDATE offline_exam_submissions SET isSynced = 1 WHERE localId = :localId")
    void markSubmissionSynced(long localId);

    @Query("DELETE FROM offline_exam_submissions WHERE isSynced = 1")
    void deleteSyncedSubmissions();
}
