package com.questions.govt.data.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Question Model compatible with Room DB and Laravel Sanctum QuestionResource.
 */
@Entity(tableName = "questions")
public class Question implements Serializable {

    @PrimaryKey
    @SerializedName("id")
    private long id;

    @SerializedName("question_text")
    private String questionText;

    @SerializedName("title")
    private String title;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("option_a")
    private String optionA;

    @SerializedName("option_b")
    private String optionB;

    @SerializedName("option_c")
    private String optionC;

    @SerializedName("option_d")
    private String optionD;

    @SerializedName("correct_option")
    private String correctOption; // "A", "B", "C", "D"

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("explanation_image_url")
    private String explanationImageUrl;

    @SerializedName("subject_id")
    private long subjectId;

    @SerializedName("subject_title")
    private String subjectTitle;

    @SerializedName("category_id")
    private long categoryId;

    @SerializedName("category_title")
    private String categoryTitle;

    @SerializedName("topic_id")
    private long topicId;

    @SerializedName("topic_title")
    private String topicTitle;

    @SerializedName("difficulty")
    private String difficulty = "medium"; // "easy", "medium", "hard"

    @SerializedName("creator_id")
    private long creatorId;

    @SerializedName("creator_name")
    private String creatorName;

    @SerializedName("creator_username")
    private String creatorUsername;

    @SerializedName("creator_avatar_url")
    private String creatorAvatarUrl;

    @SerializedName("publication_status")
    private String publicationStatus = "published";

    // Telemetry fields
    @SerializedName("views_count")
    private int viewsCount;

    @SerializedName("attempts_count")
    private int attemptsCount;

    @SerializedName("stars_count")
    private int starsCount;

    @SerializedName("comments_count")
    private int commentsCount;

    @SerializedName("option_a_pct")
    private float optionAPct;

    @SerializedName("option_b_pct")
    private float optionBPct;

    @SerializedName("option_c_pct")
    private float optionCPct;

    @SerializedName("option_d_pct")
    private float optionDPct;

    @SerializedName("upvotes_count")
    private int upvotesCount;

    @SerializedName("downvotes_count")
    private int downvotesCount;

    @SerializedName("is_starred")
    private boolean isStarred;

    @SerializedName("user_voted")
    private String userVoted; // "A", "B", "C", "D"

    @SerializedName("is_mistake")
    private boolean isMistake;

    @SerializedName("tags_string")
    private String tagsString;

    @Ignore
    @SerializedName("tags")
    private List<Object> rawTags;

    // Nested objects from Laravel QuestionResource
    @Ignore
    @SerializedName("author")
    private QuestionAuthor author;

    @Ignore
    @SerializedName("subject")
    private TaxonomyNested subject;

    @Ignore
    @SerializedName("category")
    private TaxonomyNested category;

    @Ignore
    @SerializedName("topic")
    private TaxonomyNested topic;

    @Ignore
    @SerializedName("metrics")
    private QuestionMetrics metrics;

    @Ignore
    @SerializedName("user_interactions")
    private UserInteractions userInteractions;

    // Client runtime state
    @Ignore
    private String userSelectedOption;

    @Ignore
    private boolean isAnswerRevealed;

    public Question() {
    }

    // Nested Helper Classes for Laravel JSON API
    public static class QuestionAuthor implements Serializable {
        @SerializedName("id") public Long id;
        @SerializedName("name") public String name;
        @SerializedName("username") public String username;
        @SerializedName("avatar_url") public String avatarUrl;
    }

    public static class TaxonomyNested implements Serializable {
        @SerializedName("id") public Long id;
        @SerializedName("name") public String name;
        @SerializedName("slug") public String slug;
    }

    public static class QuestionMetrics implements Serializable {
        @SerializedName("total_views") public int totalViews;
        @SerializedName("total_attempts") public int totalAttempts;
        @SerializedName("total_stars") public int totalStars;
        @SerializedName("total_comments") public int totalComments;
        @SerializedName("distribution") public Map<String, DistributionItem> distribution;
    }

    public static class DistributionItem implements Serializable {
        @SerializedName("count") public int count;
        @SerializedName("percentage") public float percentage;
    }

    public static class UserInteractions implements Serializable {
        @SerializedName("is_starred") public boolean isStarred;
        @SerializedName("my_vote") public String myVote;
        @SerializedName("my_last_attempt") public String myLastAttempt;
    }

    // Getters and Setters with smart fallbacks
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getQuestionText() {
        if (questionText != null && !questionText.isEmpty()) return questionText;
        return title != null ? title : "";
    }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getTitle() { return title != null ? title : questionText; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOptionA() { return optionA != null ? optionA : ""; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB != null ? optionB : ""; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC != null ? optionC : ""; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD != null ? optionD : ""; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectOption() { return correctOption != null ? correctOption : ""; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }

    public String getExplanation() { return explanation != null ? explanation : ""; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getExplanationImageUrl() { return explanationImageUrl; }
    public void setExplanationImageUrl(String explanationImageUrl) { this.explanationImageUrl = explanationImageUrl; }

    public long getSubjectId() {
        if (subjectId > 0) return subjectId;
        return (subject != null && subject.id != null) ? subject.id : 0;
    }
    public void setSubjectId(long subjectId) { this.subjectId = subjectId; }

    public String getSubjectTitle() {
        if (subjectTitle != null && !subjectTitle.isEmpty()) return subjectTitle;
        return (subject != null && subject.name != null) ? subject.name : "";
    }
    public void setSubjectTitle(String subjectTitle) { this.subjectTitle = subjectTitle; }

    public long getCategoryId() {
        if (categoryId > 0) return categoryId;
        return (category != null && category.id != null) ? category.id : 0;
    }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public String getCategoryTitle() {
        if (categoryTitle != null && !categoryTitle.isEmpty()) return categoryTitle;
        return (category != null && category.name != null) ? category.name : "";
    }
    public void setCategoryTitle(String categoryTitle) { this.categoryTitle = categoryTitle; }

    public long getTopicId() {
        if (topicId > 0) return topicId;
        return (topic != null && topic.id != null) ? topic.id : 0;
    }
    public void setTopicId(long topicId) { this.topicId = topicId; }

    public String getTopicTitle() {
        if (topicTitle != null && !topicTitle.isEmpty()) return topicTitle;
        return (topic != null && topic.name != null) ? topic.name : "";
    }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }

    public String getDifficulty() { return difficulty != null ? difficulty : "medium"; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public long getCreatorId() {
        if (creatorId > 0) return creatorId;
        return (author != null && author.id != null) ? author.id : 0;
    }
    public void setCreatorId(long creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() {
        if (author != null && author.name != null) return author.name;
        return creatorName != null ? creatorName : "Admin";
    }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getCreatorUsername() {
        if (author != null && author.username != null) return author.username;
        return creatorUsername != null ? creatorUsername : "admin";
    }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public String getCreatorAvatarUrl() {
        if (author != null && author.avatarUrl != null) return author.avatarUrl;
        return creatorAvatarUrl;
    }
    public void setCreatorAvatarUrl(String creatorAvatarUrl) { this.creatorAvatarUrl = creatorAvatarUrl; }

    public String getPublicationStatus() { return publicationStatus; }
    public void setPublicationStatus(String publicationStatus) { this.publicationStatus = publicationStatus; }

    public int getViewsCount() {
        if (metrics != null) return metrics.totalViews;
        return viewsCount;
    }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }

    public int getAttemptsCount() {
        if (metrics != null) return metrics.totalAttempts;
        return attemptsCount;
    }
    public void setAttemptsCount(int attemptsCount) { this.attemptsCount = attemptsCount; }

    public int getStarsCount() {
        if (metrics != null) return metrics.totalStars;
        return starsCount;
    }
    public void setStarsCount(int starsCount) { this.starsCount = starsCount; }

    public int getCommentsCount() {
        if (metrics != null) return metrics.totalComments;
        return commentsCount;
    }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public float getOptionAPct() {
        if (metrics != null && metrics.distribution != null && metrics.distribution.containsKey("A")) {
            return metrics.distribution.get("A").percentage;
        }
        return optionAPct;
    }
    public void setOptionAPct(float optionAPct) { this.optionAPct = optionAPct; }

    public float getOptionBPct() {
        if (metrics != null && metrics.distribution != null && metrics.distribution.containsKey("B")) {
            return metrics.distribution.get("B").percentage;
        }
        return optionBPct;
    }
    public void setOptionBPct(float optionBPct) { this.optionBPct = optionBPct; }

    public float getOptionCPct() {
        if (metrics != null && metrics.distribution != null && metrics.distribution.containsKey("C")) {
            return metrics.distribution.get("C").percentage;
        }
        return optionCPct;
    }
    public void setOptionCPct(float optionCPct) { this.optionCPct = optionCPct; }

    public float getOptionDPct() {
        if (metrics != null && metrics.distribution != null && metrics.distribution.containsKey("D")) {
            return metrics.distribution.get("D").percentage;
        }
        return optionDPct;
    }
    public void setOptionDPct(float optionDPct) { this.optionDPct = optionDPct; }

    public int getUpvotesCount() { return upvotesCount; }
    public void setUpvotesCount(int upvotesCount) { this.upvotesCount = upvotesCount; }

    public int getDownvotesCount() { return downvotesCount; }
    public void setDownvotesCount(int downvotesCount) { this.downvotesCount = downvotesCount; }

    public boolean isStarred() {
        if (userInteractions != null) return userInteractions.isStarred;
        return isStarred;
    }
    public void setStarred(boolean starred) { isStarred = starred; }

    public String getUserVoted() {
        if (userInteractions != null && userInteractions.myVote != null) return userInteractions.myVote;
        return userVoted;
    }
    public void setUserVoted(String userVoted) { this.userVoted = userVoted; }

    public boolean isMistake() { return isMistake; }
    public void setMistake(boolean mistake) { isMistake = mistake; }

    public String getTagsString() { return tagsString; }
    public void setTagsString(String tagsString) { this.tagsString = tagsString; }

    public List<String> getTags() {
        List<String> list = new ArrayList<>();
        if (rawTags != null) {
            for (Object obj : rawTags) {
                if (obj instanceof String) list.add((String) obj);
                else if (obj != null) list.add(obj.toString());
            }
        }
        return list;
    }

    public String getUserSelectedOption() {
        if (userSelectedOption != null) return userSelectedOption;
        if (userInteractions != null && userInteractions.myLastAttempt != null) {
            return userInteractions.myLastAttempt;
        }
        return null;
    }
    public void setUserSelectedOption(String userSelectedOption) { this.userSelectedOption = userSelectedOption; }

    public boolean isAnswerRevealed() { return isAnswerRevealed; }
    public void setAnswerRevealed(boolean answerRevealed) { isAnswerRevealed = answerRevealed; }
}
