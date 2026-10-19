package com.questions.govt.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Data model for official competitive exams created by Admin.
 * Attending these exams affects student rank points and leaderboard standing.
 */
public class OfficialExamItem implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("exam_code")
    private String examCode;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("is_official")
    private boolean isOfficial = true;

    @SerializedName("affects_ranking")
    private boolean affectsRanking = true;

    @SerializedName("total_questions")
    private int totalQuestions;

    @SerializedName("duration_minutes")
    private int durationMinutes;

    @SerializedName("duration_seconds")
    private int durationSeconds;

    @SerializedName("negative_marking")
    private double negativeMarking;

    @SerializedName("pass_percentage")
    private double passPercentage;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("subject")
    private TaxonomyItem subject;

    @SerializedName("category")
    private TaxonomyItem category;

    @SerializedName("total_attended")
    private int totalAttended;

    @SerializedName("total_attempts")
    private int totalAttempts;

    @SerializedName("total_correct")
    private int totalCorrect;

    @SerializedName("accuracy_percentage")
    private double accuracyPercentage;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getExamCode() { return examCode != null ? examCode : "EXAM-" + id; }
    public void setExamCode(String examCode) { this.examCode = examCode; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public boolean isOfficial() { return isOfficial; }
    public void setOfficial(boolean official) { isOfficial = official; }

    public boolean isAffectsRanking() { return affectsRanking; }
    public void setAffectsRanking(boolean affectsRanking) { this.affectsRanking = affectsRanking; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getDurationSeconds() { return durationSeconds > 0 ? durationSeconds : durationMinutes * 60; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public double getNegativeMarking() { return negativeMarking; }
    public void setNegativeMarking(double negativeMarking) { this.negativeMarking = negativeMarking; }

    public double getPassPercentage() { return passPercentage; }
    public void setPassPercentage(double passPercentage) { this.passPercentage = passPercentage; }

    public String getDifficulty() { return difficulty != null ? difficulty : "Medium"; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public TaxonomyItem getSubject() { return subject; }
    public void setSubject(TaxonomyItem subject) { this.subject = subject; }

    public TaxonomyItem getCategory() { return category; }
    public void setCategory(TaxonomyItem category) { this.category = category; }

    public int getTotalAttended() { return totalAttended; }
    public void setTotalAttended(int totalAttended) { this.totalAttended = totalAttended; }

    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }

    public int getTotalCorrect() { return totalCorrect; }
    public void setTotalCorrect(int totalCorrect) { this.totalCorrect = totalCorrect; }

    public double getAccuracyPercentage() { return accuracyPercentage; }
    public void setAccuracyPercentage(double accuracyPercentage) { this.accuracyPercentage = accuracyPercentage; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
