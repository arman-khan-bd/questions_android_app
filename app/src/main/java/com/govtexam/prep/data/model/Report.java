package com.govtexam.prep.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Report model for submitting question disputes to Admin Report Manager.
 */
public class Report implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("question_id")
    private long questionId;

    @SerializedName("reason")
    private String reason; // "Wrong Answer Key", "Typo", "Outdated Info", "Duplicate", "Spam"

    @SerializedName("note")
    private String note;

    @SerializedName("status")
    private String status = "pending"; // "pending", "resolved", "dismissed"

    @SerializedName("created_at")
    private String createdAt;

    public Report() {
    }

    public Report(long questionId, String reason, String note) {
        this.questionId = questionId;
        this.reason = reason;
        this.note = note;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }

    public String getReason() { return reason != null ? reason : ""; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNote() { return note != null ? note : ""; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status != null ? status : "pending"; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
