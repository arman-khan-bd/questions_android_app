package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;
import com.questions.govt.data.model.Question;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result payload from POST /api/v1/exams/custom-generate.
 */
public class CustomExamGenerateResponse implements Serializable {

    @SerializedName("exam_id")
    private long examId;

    @SerializedName("title")
    private String title;

    @SerializedName("total_questions")
    private int totalQuestions;

    @SerializedName("duration_minutes")
    private int durationMinutes;

    @SerializedName("negative_marking")
    private double negativeMarking;

    @SerializedName("questions")
    private List<Question> questions = new ArrayList<>();

    public long getExamId() { return examId; }
    public void setExamId(long examId) { this.examId = examId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getNegativeMarking() { return negativeMarking; }
    public void setNegativeMarking(double negativeMarking) { this.negativeMarking = negativeMarking; }

    public List<Question> getQuestions() { return questions != null ? questions : new ArrayList<>(); }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}
