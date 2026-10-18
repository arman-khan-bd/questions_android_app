package com.questions.govt.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Local Room entity caching app configuration, dynamic branding, multipliers, and feature flags.
 */
@Entity(tableName = "app_config")
public class AppConfigEntity implements Serializable {

    @PrimaryKey
    private int id = 1; // Single row table

    private String appName = "Govt Exam Prep";
    private String tagline = "Master Preliminary MCQ Exams with Precision";
    private String logoUrl;
    private String headerLogoUrl;

    private boolean maintenanceMode = false;
    private String maintenanceMessage;
    private String maintenanceBannerUrl;
    private String estimatedBackAt;

    private int latestVersionCode = 1;
    private String latestVersionName = "1.0.0";
    private String updateMode = "none";
    private String updateDownloadUrl;
    private String releaseNotes;

    private boolean enableUserRegistration = true;
    private boolean enableCommunityQuestionCreation = true;
    private boolean enableQuestionComments = true;
    private boolean enableCommentReplies = true;
    private boolean enableAnswerVotingSystem = true;
    private boolean enableCustomExamBuilder = true;
    private boolean enableLeaderboard = true;
    private boolean autoPublishUserQuestions = false;

    private double easyMultiplier = 1.0;
    private double mediumMultiplier = 1.5;
    private double hardMultiplier = 2.5;
    private String negativeMarkingPresetsJson; // e.g. "[0.0, -0.25, -0.5]"

    public AppConfigEntity() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getTagline() { return tagline; }
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

    public String getUpdateMode() { return updateMode; }
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

    public double getEasyMultiplier() { return easyMultiplier; }
    public void setEasyMultiplier(double easyMultiplier) { this.easyMultiplier = easyMultiplier; }

    public double getMediumMultiplier() { return mediumMultiplier; }
    public void setMediumMultiplier(double mediumMultiplier) { this.mediumMultiplier = mediumMultiplier; }

    public double getHardMultiplier() { return hardMultiplier; }
    public void setHardMultiplier(double hardMultiplier) { this.hardMultiplier = hardMultiplier; }

    public String getNegativeMarkingPresetsJson() { return negativeMarkingPresetsJson; }
    public void setNegativeMarkingPresetsJson(String negativeMarkingPresetsJson) { this.negativeMarkingPresetsJson = negativeMarkingPresetsJson; }
}
