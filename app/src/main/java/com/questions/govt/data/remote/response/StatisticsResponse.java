package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Statistics metrics payload from GET /api/v1/statistics matching Laravel ProfileApiController.
 */
public class StatisticsResponse implements Serializable {

    // Nested payload from Laravel
    @SerializedName("exams")
    private ExamsSummary exams;

    @SerializedName("chart_data")
    private ChartData chartData;

    // Direct / flat payload fallbacks
    @SerializedName("total_exams_attended")
    private int totalExamsAttended;

    @SerializedName("exams_passed")
    private int examsPassed;

    @SerializedName("exams_failed")
    private int examsFailed;

    @SerializedName("easy_attempted")
    private int easyAttempted;
    @SerializedName("easy_correct")
    private int easyCorrect;
    @SerializedName("easy_wrong")
    private int easyWrong;

    @SerializedName("medium_attempted")
    private int mediumAttempted;
    @SerializedName("medium_correct")
    private int mediumCorrect;
    @SerializedName("medium_wrong")
    private int mediumWrong;

    @SerializedName("hard_attempted")
    private int hardAttempted;
    @SerializedName("hard_correct")
    private int hardCorrect;
    @SerializedName("hard_wrong")
    private int hardWrong;

    @SerializedName("score_progression")
    private List<ExamScorePoint> scoreProgression = new ArrayList<>();

    @SerializedName("subject_mastery")
    private List<SubjectMasteryItem> subjectMastery = new ArrayList<>();

    public static class ExamsSummary implements Serializable {
        @SerializedName("total_attended") public int totalAttended;
        @SerializedName("passed") public int passed;
        @SerializedName("failed") public int failed;
        @SerializedName("average_accuracy") public float averageAccuracy;
        @SerializedName("average_score") public float averageScore;
        @SerializedName("total_negative_marks") public float totalNegativeMarks;
    }

    public static class ChartData implements Serializable {
        @SerializedName("difficulty_distribution") public Map<String, Integer> difficultyDistribution;
        @SerializedName("streak_days") public int streakDays;
    }

    public static class ExamScorePoint implements Serializable {
        @SerializedName("exam_title")
        private String examTitle;
        @SerializedName("accuracy")
        private float accuracy;
        @SerializedName("score")
        private float score;

        public String getExamTitle() { return examTitle != null ? examTitle : ""; }
        public float getAccuracy() { return accuracy; }
        public float getScore() { return score; }
    }

    public static class SubjectMasteryItem implements Serializable {
        @SerializedName("subject_id")
        private long subjectId;
        @SerializedName("subject_title")
        private String subjectTitle;
        @SerializedName("accuracy_percentage")
        private float accuracyPercentage;
        @SerializedName("questions_attempted")
        private int questionsAttempted;

        public long getSubjectId() { return subjectId; }
        public String getSubjectTitle() { return subjectTitle != null ? subjectTitle : ""; }
        public float getAccuracyPercentage() { return accuracyPercentage; }
        public int getQuestionsAttempted() { return questionsAttempted; }
    }

    // Smart Getters
    public int getTotalExamsAttended() {
        if (exams != null) return exams.totalAttended;
        return totalExamsAttended;
    }

    public int getExamsPassed() {
        if (exams != null) return exams.passed;
        return examsPassed;
    }

    public int getExamsFailed() {
        if (exams != null) return exams.failed;
        return examsFailed;
    }

    public float getAverageAccuracy() {
        if (exams != null) return exams.averageAccuracy;
        return 0f;
    }

    public int getEasyAttempted() { return easyAttempted; }

    public int getEasyCorrect() {
        if (chartData != null && chartData.difficultyDistribution != null && chartData.difficultyDistribution.containsKey("Easy")) {
            return chartData.difficultyDistribution.get("Easy");
        }
        return easyCorrect;
    }

    public int getEasyWrong() { return easyWrong; }

    public int getMediumAttempted() { return mediumAttempted; }

    public int getMediumCorrect() {
        if (chartData != null && chartData.difficultyDistribution != null && chartData.difficultyDistribution.containsKey("Medium")) {
            return chartData.difficultyDistribution.get("Medium");
        }
        return mediumCorrect;
    }

    public int getMediumWrong() { return mediumWrong; }

    public int getHardAttempted() { return hardAttempted; }

    public int getHardCorrect() {
        if (chartData != null && chartData.difficultyDistribution != null && chartData.difficultyDistribution.containsKey("Hard")) {
            return chartData.difficultyDistribution.get("Hard");
        }
        return hardCorrect;
    }

    public int getHardWrong() { return hardWrong; }

    public List<ExamScorePoint> getScoreProgression() {
        return scoreProgression != null ? scoreProgression : new ArrayList<>();
    }

    public List<SubjectMasteryItem> getSubjectMastery() {
        return subjectMastery != null ? subjectMastery : new ArrayList<>();
    }
}
