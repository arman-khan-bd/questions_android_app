package com.questions.govt.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Question comment model supporting 2-level nested replies and backend bad-word moderation statuses.
 */
public class Comment implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("question_id")
    private long questionId;

    @SerializedName("user_id")
    private long userId;

    @SerializedName("user_name")
    private String userName;

    @SerializedName("user_username")
    private String userUsername;

    @SerializedName("user_avatar_url")
    private String userAvatarUrl;

    @SerializedName("user")
    private CommentUser author;

    @SerializedName("comment_text")
    private String commentText;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("moderation_status")
    private String moderationStatus = "clean"; // "clean", "masked", "flagged_pending"

    @SerializedName("status")
    private String status;

    @SerializedName("parent_id")
    private Long parentId;

    @SerializedName("replies")
    private List<Comment> replies = new ArrayList<>();

    public Comment() {
    }

    public static class CommentUser implements Serializable {
        @SerializedName("id") public Long id;
        @SerializedName("name") public String name;
        @SerializedName("username") public String username;
        @SerializedName("avatar_url") public String avatarUrl;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }

    public long getUserId() {
        if (userId > 0) return userId;
        return (author != null && author.id != null) ? author.id : 0;
    }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUserName() {
        if (author != null && author.name != null) return author.name;
        return userName != null ? userName : "Student";
    }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserUsername() {
        if (author != null && author.username != null) return author.username;
        return userUsername != null ? userUsername : "";
    }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }

    public String getUserAvatarUrl() {
        if (author != null && author.avatarUrl != null) return author.avatarUrl;
        return userAvatarUrl;
    }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }

    public String getCommentText() { return commentText != null ? commentText : ""; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getModerationStatus() {
        if (status != null && !status.isEmpty()) return status;
        return moderationStatus != null ? moderationStatus : "clean";
    }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public List<Comment> getReplies() {
        if (replies == null) {
            replies = new ArrayList<>();
        }
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies != null ? replies : new ArrayList<>();
    }

    public void addReply(Comment reply) {
        if (replies == null) {
            replies = new ArrayList<>();
        }
        replies.add(reply);
    }
}
