package com.questions.govt.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Entry on global leaderboard or top question creators list,
 * matching Laravel's ProfileApiController::leaderboard.
 */
public class LeaderboardEntry implements Serializable {

    @SerializedName("user_id")
    private long userId;

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("username")
    private String username;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("tier_badge")
    private String tierBadge;

    @SerializedName("target_exam")
    private String targetExam;

    @SerializedName("rank")
    private int rank;

    @SerializedName("score")
    private double score;

    @SerializedName("rank_points")
    private double rankPoints;

    @SerializedName("difficulty_badge")
    private String difficultyBadge;

    @SerializedName("exams_attended_count")
    private int examsAttendedCount;

    @SerializedName("hard_ratio")
    private float hardRatio;

    @SerializedName("accuracy_rate")
    private float accuracyRate;

    @SerializedName("streak_days")
    private int streakDays;

    @SerializedName("questions_created_count")
    private int questionsCreatedCount;

    public LeaderboardEntry() {
    }

    public long getUserId() {
        if (userId > 0) return userId;
        return id;
    }
    public void setUserId(long userId) { this.userId = userId; }

    public long getId() {
        if (id > 0) return id;
        return userId;
    }
    public void setId(long id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username != null ? username : ""; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getTierBadge() {
        if (targetExam != null && !targetExam.isEmpty()) return targetExam;
        return tierBadge != null ? tierBadge : "Aspirant";
    }
    public void setTierBadge(String tierBadge) { this.tierBadge = tierBadge; }

    public String getTargetExam() { return targetExam; }
    public void setTargetExam(String targetExam) { this.targetExam = targetExam; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public double getScore() {
        if (rankPoints > 0) return rankPoints;
        return score;
    }
    public void setScore(double score) { this.score = score; }

    public double getRankPoints() { return rankPoints; }
    public void setRankPoints(double rankPoints) { this.rankPoints = rankPoints; }

    public String getDifficultyBadge() { return difficultyBadge; }
    public void setDifficultyBadge(String difficultyBadge) { this.difficultyBadge = difficultyBadge; }

    public int getExamsAttendedCount() { return examsAttendedCount; }
    public void setExamsAttendedCount(int examsAttendedCount) { this.examsAttendedCount = examsAttendedCount; }

    public float getHardRatio() { return hardRatio; }
    public void setHardRatio(float hardRatio) { this.hardRatio = hardRatio; }

    public float getAccuracyRate() { return accuracyRate; }
    public void setAccuracyRate(float accuracyRate) { this.accuracyRate = accuracyRate; }

    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }

    public int getQuestionsCreatedCount() { return questionsCreatedCount; }
    public void setQuestionsCreatedCount(int questionsCreatedCount) { this.questionsCreatedCount = questionsCreatedCount; }
}
