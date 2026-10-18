package com.govtexam.prep.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Result metrics for a completed exam session.
 */
@Entity(tableName = "exam_results")
public class ExamResult implements Serializable {

    @PrimaryKey
    @SerializedName("id")
    private long id;

    @SerializedName("exam_id")
    private long examId;

    @SerializedName("title")
    private String title;

    @SerializedName("total_questions")
    private int totalQuestions;

    @SerializedName("attempted_count")
    private int attemptedCount;

    @SerializedName("correct_count")
    private int correctCount;

    @SerializedName("wrong_count")
    private int wrongCount;

    @SerializedName("skipped_count")
    private int skippedCount;

    @SerializedName("raw_score")
    private double rawScore;

    @SerializedName("negative_deduction")
    private double negativeDeduction;

    @SerializedName("final_score")
    private double finalScore;

    @SerializedName("accuracy_percentage")
    private float accuracyPercentage;

    @SerializedName("time_spent_seconds")
    private long timeSpentSeconds;

    @SerializedName("is_passed")
    private boolean isPassed;

    @SerializedName("points_earned")
    private double pointsEarned;

    @SerializedName("global_rank")
    private int globalRank;

    @SerializedName("completed_at")
    private String completedAt;

    public ExamResult() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }

    public String getTitle() { return title != null ? title : "Exam Result"; }
    public void setTitle(String title) { this.title = title; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getAttemptedCount() { return attemptedCount; }
    public void setAttemptedCount(int attemptedCount) { this.attemptedCount = attemptedCount; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }

    public int getWrongCount() { return wrongCount; }
    public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }

    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }

    public double getRawScore() { return rawScore; }
    public void setRawScore(double rawScore) { this.rawScore = rawScore; }

    public double getNegativeDeduction() { return negativeDeduction; }
    public void setNegativeDeduction(double negativeDeduction) { this.negativeDeduction = negativeDeduction; }

    public double getFinalScore() { return finalScore; }
    public void setFinalScore(double finalScore) { this.finalScore = finalScore; }

    public float getAccuracyPercentage() { return accuracyPercentage; }
    public void setAccuracyPercentage(float accuracyPercentage) { this.accuracyPercentage = accuracyPercentage; }

    public long getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(long timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public boolean isPassed() { return isPassed; }
    public void setPassed(boolean passed) { isPassed = passed; }

    public double getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(double pointsEarned) { this.pointsEarned = pointsEarned; }

    public int getGlobalRank() { return globalRank; }
    public void setGlobalRank(int globalRank) { this.globalRank = globalRank; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
