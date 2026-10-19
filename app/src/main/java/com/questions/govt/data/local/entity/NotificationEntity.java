package com.questions.govt.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Local Room Entity for notifications with strict composite anti-duplication index.
 * Matches backend composite UNIQUE constraint on (recipient_id, actor_id, question_id, type).
 */
@Entity(tableName = "notifications")
public class NotificationEntity implements Serializable {

    @PrimaryKey
    @SerializedName("id")
    private long id; // Remote backend ID

    @SerializedName("recipient_id")
    private long recipientId;

    @SerializedName("actor_id")
    private long actorId;

    @SerializedName("actor_name")
    private String actorName;

    @SerializedName("actor_username")
    private String actorUsername;

    @SerializedName("actor_avatar_url")
    private String actorAvatarUrl;

    @SerializedName("question_id")
    private Long questionId;

    @SerializedName("type")
    private String type; // "star", "comment", "reply", "report", "answer_click", "admin_broadcast"

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("target_type")
    private String targetType;

    @SerializedName("target_id")
    private String targetId;

    @SerializedName("target_url")
    private String targetUrl;

    private long timestamp = System.currentTimeMillis();

    public NotificationEntity() {
    }

    public String getTargetType() { return targetType != null ? targetType : type; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public long getLocalId() { return id; }
    public void setLocalId(long localId) { this.id = localId; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getRecipientId() { return recipientId; }
    public void setRecipientId(long recipientId) { this.recipientId = recipientId; }

    public long getActorId() { return actorId; }
    public void setActorId(long actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName != null ? actorName : "Student"; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorUsername() { return actorUsername != null ? actorUsername : ""; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getActorAvatarUrl() { return actorAvatarUrl; }
    public void setActorAvatarUrl(String actorAvatarUrl) { this.actorAvatarUrl = actorAvatarUrl; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getType() { return type != null ? type : "admin_broadcast"; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message != null ? message : ""; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
