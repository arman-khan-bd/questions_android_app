package com.questions.govt.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Report filed against a question, reviewed by Admin and question creator.
 */
public class ReportItem implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("question_id")
    private long questionId;

    @SerializedName("question_text")
    private String questionText;

    @SerializedName("reason")
    private String reason; // "Wrong Answer Key", "Typo", "Outdated Info", "Duplicate", "Spam"

    @SerializedName("note")
    private String note;

    @SerializedName("status")
    private String status = "pending"; // "pending", "resolved", "dismissed"

    @SerializedName("created_at")
    private String createdAt;

    public ReportItem() {
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }

    public String getQuestionText() { return questionText != null ? questionText : ""; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getReason() { return reason != null ? reason : ""; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNote() { return note != null ? note : ""; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status != null ? status : "pending"; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
