package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;
import com.questions.govt.data.model.LeaderboardEntry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result payload from GET /api/v1/leaderboard.
 */
public class LeaderboardResponse implements Serializable {

    @SerializedName("mode")
    private String mode; // "easy", "medium", "custom", "creators"

    @SerializedName("tier")
    private String tier; // "all", "easy", "medium", "hard"

    @SerializedName("timeframe")
    private String timeframe; // "today", "weekly", "monthly", "all_time"

    @SerializedName("multipliers")
    private Map<String, Double> multipliers;

    @SerializedName("top_podium")
    private List<LeaderboardEntry> topPodium = new ArrayList<>();

    @SerializedName("leaderboard")
    private List<LeaderboardEntry> leaderboard = new ArrayList<>();

    @SerializedName("current_user_entry")
    private LeaderboardEntry currentUserEntry;

    public String getMode() { return mode != null ? mode : tier; }
    public void setMode(String mode) { this.mode = mode; }

    public String getTier() { return tier != null ? tier : mode; }
    public void setTier(String tier) { this.tier = tier; }

    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }

    public Map<String, Double> getMultipliers() { return multipliers; }
    public void setMultipliers(Map<String, Double> multipliers) { this.multipliers = multipliers; }

    public List<LeaderboardEntry> getTopPodium() {
        if (topPodium != null && !topPodium.isEmpty()) return topPodium;
        if (leaderboard != null && leaderboard.size() >= 3) {
            return leaderboard.subList(0, 3);
        }
        return new ArrayList<>();
    }
    public void setTopPodium(List<LeaderboardEntry> topPodium) { this.topPodium = topPodium; }

    public List<LeaderboardEntry> getLeaderboard() { return leaderboard != null ? leaderboard : new ArrayList<>(); }
    public void setLeaderboard(List<LeaderboardEntry> leaderboard) { this.leaderboard = leaderboard; }

    public LeaderboardEntry getCurrentUserEntry() { return currentUserEntry; }
    public void setCurrentUserEntry(LeaderboardEntry currentUserEntry) { this.currentUserEntry = currentUserEntry; }
}
