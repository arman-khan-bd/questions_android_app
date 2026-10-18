package com.govtexam.prep.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Remote app configuration, dynamic branding, feature flags, and multipliers
 * delivered by GET /api/v1/app-config.
 */
public class AppConfigData implements Serializable {

    // 1. Dynamic Branding
    @SerializedName("app_name")
    private String appName = "Govt Exam Prep";

    @SerializedName("tagline")
    private String tagline = "Master Preliminary MCQ Exams with Precision";

    @SerializedName("logo_url")
    private String logoUrl;

    @SerializedName("header_logo_url")
    private String headerLogoUrl;

    // 2. Maintenance Mode
    @SerializedName("maintenance_mode")
    private boolean maintenanceMode = false;

    @SerializedName("maintenance_message")
    private String maintenanceMessage;

    @SerializedName("maintenance_banner_url")
    private String maintenanceBannerUrl;

    @SerializedName("estimated_back_at")
    private String estimatedBackAt;

    // 3. Version Update Controller (OTA)
    @SerializedName("latest_version_code")
    private int latestVersionCode = 1;

    @SerializedName("latest_version_name")
    private String latestVersionName = "1.0.0";

    @SerializedName("update_mode")
    private String updateMode = "none"; // "none", "optional", "force"

    @SerializedName("update_download_url")
    private String updateDownloadUrl;

    @SerializedName("release_notes")
    private String releaseNotes;

    // 4. Remote Feature Flags
    @SerializedName("enable_user_registration")
    private boolean enableUserRegistration = true;

    @SerializedName("enable_community_question_creation")
    private boolean enableCommunityQuestionCreation = true;

    @SerializedName("enable_question_comments")
    private boolean enableQuestionComments = true;

    @SerializedName("enable_comment_replies")
    private boolean enableCommentReplies = true;

    @SerializedName("enable_answer_voting_system")
    private boolean enableAnswerVotingSystem = true;

    @SerializedName("enable_custom_exam_builder")
    private boolean enableCustomExamBuilder = true;

    @SerializedName("enable_leaderboard")
    private boolean enableLeaderboard = true;

    @SerializedName("auto_publish_user_questions")
    private boolean autoPublishUserQuestions = false;

    // 5. Dynamic Exam & Leaderboard Multipliers
    @SerializedName("easy_multiplier")
    private double easyMultiplier = 1.0;

    @SerializedName("medium_multiplier")
    private double mediumMultiplier = 1.5;

    @SerializedName("hard_multiplier")
    private double hardMultiplier = 2.5;

    @SerializedName("negative_marking_presets")
    private List<Double> negativeMarkingPresets;

    public AppConfigData() {
        negativeMarkingPresets = new ArrayList<>();
        negativeMarkingPresets.add(0.00);
        negativeMarkingPresets.add(-0.25);
        negativeMarkingPresets.add(-0.50);
    }

    // Getters and Setters
    public String getAppName() { return appName != null ? appName : "Govt Exam Prep"; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getTagline() { return tagline != null ? tagline : ""; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getHeaderLogoUrl() { return headerLogoUrl; }
    public void setHeaderLogoUrl(String headerLogoUrl) { this.headerLogoUrl = headerLogoUrl; }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public String getMaintenanceMessage() { return maintenanceMessage; }
    public void setMaintenanceMessage(String maintenanceMessage) { this.maintenanceMessage = maintenanceMessage; }

    public String getMaintenanceBannerUrl() { return maintenanceBannerUrl; }
    public void setMaintenanceBannerUrl(String maintenanceBannerUrl) { this.maintenanceBannerUrl = maintenanceBannerUrl; }

    public String getEstimatedBackAt() { return estimatedBackAt; }
    public void setEstimatedBackAt(String estimatedBackAt) { this.estimatedBackAt = estimatedBackAt; }

    public int getLatestVersionCode() { return latestVersionCode; }
    public void setLatestVersionCode(int latestVersionCode) { this.latestVersionCode = latestVersionCode; }

    public String getLatestVersionName() { return latestVersionName; }
    public void setLatestVersionName(String latestVersionName) { this.latestVersionName = latestVersionName; }

    public String getUpdateMode() { return updateMode != null ? updateMode : "none"; }
    public void setUpdateMode(String updateMode) { this.updateMode = updateMode; }

    public String getUpdateDownloadUrl() { return updateDownloadUrl; }
    public void setUpdateDownloadUrl(String updateDownloadUrl) { this.updateDownloadUrl = updateDownloadUrl; }

    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }

    public boolean isEnableUserRegistration() { return enableUserRegistration; }
    public void setEnableUserRegistration(boolean enableUserRegistration) { this.enableUserRegistration = enableUserRegistration; }

    public boolean isEnableCommunityQuestionCreation() { return enableCommunityQuestionCreation; }
    public void setEnableCommunityQuestionCreation(boolean enableCommunityQuestionCreation) { this.enableCommunityQuestionCreation = enableCommunityQuestionCreation; }

    public boolean isEnableQuestionComments() { return enableQuestionComments; }
    public void setEnableQuestionComments(boolean enableQuestionComments) { this.enableQuestionComments = enableQuestionComments; }

    public boolean isEnableCommentReplies() { return enableCommentReplies; }
    public void setEnableCommentReplies(boolean enableCommentReplies) { this.enableCommentReplies = enableCommentReplies; }

    public boolean isEnableAnswerVotingSystem() { return enableAnswerVotingSystem; }
    public void setEnableAnswerVotingSystem(boolean enableAnswerVotingSystem) { this.enableAnswerVotingSystem = enableAnswerVotingSystem; }

    public boolean isEnableCustomExamBuilder() { return enableCustomExamBuilder; }
    public void setEnableCustomExamBuilder(boolean enableCustomExamBuilder) { this.enableCustomExamBuilder = enableCustomExamBuilder; }

    public boolean isEnableLeaderboard() { return enableLeaderboard; }
    public void setEnableLeaderboard(boolean enableLeaderboard) { this.enableLeaderboard = enableLeaderboard; }

    public boolean isAutoPublishUserQuestions() { return autoPublishUserQuestions; }
    public void setAutoPublishUserQuestions(boolean autoPublishUserQuestions) { this.autoPublishUserQuestions = autoPublishUserQuestions; }

    public double getEasyMultiplier() { return easyMultiplier > 0 ? easyMultiplier : 1.0; }
    public void setEasyMultiplier(double easyMultiplier) { this.easyMultiplier = easyMultiplier; }

    public double getMediumMultiplier() { return mediumMultiplier > 0 ? mediumMultiplier : 1.5; }
    public void setMediumMultiplier(double mediumMultiplier) { this.mediumMultiplier = mediumMultiplier; }

    public double getHardMultiplier() { return hardMultiplier > 0 ? hardMultiplier : 2.5; }
    public void setHardMultiplier(double hardMultiplier) { this.hardMultiplier = hardMultiplier; }

    public List<Double> getNegativeMarkingPresets() {
        if (negativeMarkingPresets == null || negativeMarkingPresets.isEmpty()) {
            List<Double> fallback = new ArrayList<>();
            fallback.add(0.00);
            fallback.add(-0.25);
            fallback.add(-0.50);
            return fallback;
        }
        return negativeMarkingPresets;
    }
    public void setNegativeMarkingPresets(List<Double> negativeMarkingPresets) { this.negativeMarkingPresets = negativeMarkingPresets; }
}
