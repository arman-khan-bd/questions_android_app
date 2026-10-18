package com.govtexam.prep.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Stores exam submissions locally if the network connection drops at exam conclusion.
 * Automatically synced by ExamRepository when connection is re-established.
 */
@Entity(tableName = "offline_exam_submissions")
public class OfflineExamSubmission implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long localId;

    private long examId;
    private String examTitle;
    private String answersJson; // Serialized map of questionId -> selectedOption
    private long timeSpentSeconds;
    private long timestamp = System.currentTimeMillis();
    private boolean isSynced = false;

    public OfflineExamSubmission() {
    }

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }

    public String getExamTitle() { return examTitle != null ? examTitle : ""; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }

    public String getAnswersJson() { return answersJson != null ? answersJson : "{}"; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }

    public long getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(long timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
}
