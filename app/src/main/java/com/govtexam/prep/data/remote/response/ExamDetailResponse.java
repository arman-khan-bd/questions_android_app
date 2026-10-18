package com.govtexam.prep.data.remote.response;

import com.google.gson.annotations.SerializedName;
import com.govtexam.prep.data.model.Question;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result payload from GET /api/v1/exams/{id}.
 */
public class ExamDetailResponse implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("title")
    private String title;

    @SerializedName("duration_minutes")
    private int durationMinutes;

    @SerializedName("negative_marking")
    private double negativeMarking;

    @SerializedName("questions")
    private List<Question> questions = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getNegativeMarking() { return negativeMarking; }
    public void setNegativeMarking(double negativeMarking) { this.negativeMarking = negativeMarking; }

    public List<Question> getQuestions() { return questions != null ? questions : new ArrayList<>(); }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}
