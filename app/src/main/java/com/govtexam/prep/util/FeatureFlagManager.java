package com.govtexam.prep.util;

import com.google.gson.Gson;
import com.govtexam.prep.data.local.SessionManager;
import com.govtexam.prep.data.model.AppConfigData;

import java.util.List;

/**
 * Singleton manager to parse and reactively provide remote feature flags, dynamic branding,
 * and exam score multipliers across all Android UI components without app updates.
 */
public class FeatureFlagManager {

    private static FeatureFlagManager instance;
    private AppConfigData currentConfig;
    private final Gson gson;

    private FeatureFlagManager() {
        this.gson = new Gson();
        // Load initial state from cached preferences
        String cachedJson = SessionManager.getInstance().getCachedConfig();
        if (cachedJson != null && !cachedJson.isEmpty()) {
            try {
                this.currentConfig = gson.fromJson(cachedJson, AppConfigData.class);
            } catch (Exception ignored) {
            }
        }
        if (this.currentConfig == null) {
            this.currentConfig = new AppConfigData();
        }
    }

    public static synchronized FeatureFlagManager getInstance() {
        if (instance == null) {
            instance = new FeatureFlagManager();
        }
        return instance;
    }

    /**
     * Updates and caches remote configuration received from GET /api/v1/app-config.
     */
    public synchronized void updateConfig(AppConfigData newConfig) {
        if (newConfig != null) {
            this.currentConfig = newConfig;
            SessionManager.getInstance().saveCachedConfig(gson.toJson(newConfig));
        }
    }

    public AppConfigData getConfig() {
        return currentConfig;
    }

    // Feature Flags
    public boolean isUserRegistrationEnabled() {
        return currentConfig.isEnableUserRegistration();
    }

    public boolean isCommunityQuestionCreationEnabled() {
        return currentConfig.isEnableCommunityQuestionCreation();
    }

    public boolean isQuestionCommentsEnabled() {
        return currentConfig.isEnableQuestionComments();
    }

    public boolean isCommentRepliesEnabled() {
        return currentConfig.isEnableCommentReplies();
    }

    public boolean isAnswerVotingSystemEnabled() {
        return currentConfig.isEnableAnswerVotingSystem();
    }

    public boolean isCustomExamBuilderEnabled() {
        return currentConfig.isEnableCustomExamBuilder();
    }

    public boolean isLeaderboardEnabled() {
        return currentConfig.isEnableLeaderboard();
    }

    public boolean isAutoPublishUserQuestions() {
        return currentConfig.isAutoPublishUserQuestions();
    }

    // Multipliers & Presets
    public double getEasyMultiplier() {
        return currentConfig.getEasyMultiplier();
    }

    public double getMediumMultiplier() {
        return currentConfig.getMediumMultiplier();
    }

    public double getHardMultiplier() {
        return currentConfig.getHardMultiplier();
    }

    public List<Double> getNegativeMarkingPresets() {
        return currentConfig.getNegativeMarkingPresets();
    }

    // Dynamic Branding
    public String getAppName() {
        return currentConfig.getAppName();
    }

    public String getTagline() {
        return currentConfig.getTagline();
    }

    public String getLogoUrl() {
        return currentConfig.getLogoUrl();
    }

    public String getHeaderLogoUrl() {
        return currentConfig.getHeaderLogoUrl();
    }
}
