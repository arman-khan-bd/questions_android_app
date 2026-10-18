package com.questions.govt.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameters for POST /api/v1/exams/custom-generate.
 */
public class CustomExamRequest implements Serializable {

    @SerializedName("title")
    private String title;

    @SerializedName("subject_ids")
    private List<Long> subjectIds = new ArrayList<>();

    @SerializedName("category_ids")
    private List<Long> categoryIds = new ArrayList<>();

    @SerializedName("topic_ids")
    private List<Long> topicIds = new ArrayList<>();

    @SerializedName("tag_names")
    private List<String> tagNames = new ArrayList<>();

    @SerializedName("difficulty")
    private String difficulty = "all"; // "easy", "medium", "hard", "all"

    @SerializedName("negative_marking")
    private double negativeMarking = -0.25;

    @SerializedName("total_questions")
    private int totalQuestions = 25;

    @SerializedName("duration_minutes")
    private int durationMinutes = 20;

    public CustomExamRequest() {
    }

    public String getTitle() { return title != null ? title : "Custom Exam"; }
    public void setTitle(String title) { this.title = title; }

    public List<Long> getSubjectIds() { return subjectIds != null ? subjectIds : new ArrayList<>(); }
    public void setSubjectIds(List<Long> subjectIds) { this.subjectIds = subjectIds; }

    public List<Long> getCategoryIds() { return categoryIds != null ? categoryIds : new ArrayList<>(); }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }

    public List<Long> getTopicIds() { return topicIds != null ? topicIds : new ArrayList<>(); }
    public void setTopicIds(List<Long> topicIds) { this.topicIds = topicIds; }

    public List<String> getTagNames() { return tagNames != null ? tagNames : new ArrayList<>(); }
    public void setTagNames(List<String> tagNames) { this.tagNames = tagNames; }

    public String getDifficulty() { return difficulty != null ? difficulty : "all"; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public double getNegativeMarking() { return negativeMarking; }
    public void setNegativeMarking(double negativeMarking) { this.negativeMarking = negativeMarking; }

    public double getNegativeMark() { return getNegativeMarking(); }
    public void setNegativeMark(double negativeMark) { setNegativeMarking(negativeMark); }

    public int getTotalQuestions() { return totalQuestions > 0 ? totalQuestions : 25; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getDurationMinutes() { return durationMinutes > 0 ? durationMinutes : 20; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}
