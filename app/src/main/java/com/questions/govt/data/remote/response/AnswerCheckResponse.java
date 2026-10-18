package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Result of checking a MCQ answer on POST /api/v1/questions/{id}/answer.
 */
public class AnswerCheckResponse implements Serializable {

    @SerializedName("is_correct")
    private boolean isCorrect;

    @SerializedName("correct_option")
    private String correctOption; // "A", "B", "C", "D"

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("explanation_image_url")
    private String explanationImageUrl;

    @SerializedName("option_a_pct")
    private float optionAPct;

    @SerializedName("option_b_pct")
    private float optionBPct;

    @SerializedName("option_c_pct")
    private float optionCPct;

    @SerializedName("option_d_pct")
    private float optionDPct;

    @SerializedName("attempts_count")
    private int attemptsCount;

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getExplanationImageUrl() { return explanationImageUrl; }
    public void setExplanationImageUrl(String explanationImageUrl) { this.explanationImageUrl = explanationImageUrl; }

    public float getOptionAPct() { return optionAPct; }
    public void setOptionAPct(float optionAPct) { this.optionAPct = optionAPct; }

    public float getOptionBPct() { return optionBPct; }
    public void setOptionBPct(float optionBPct) { this.optionBPct = optionBPct; }

    public float getOptionCPct() { return optionCPct; }
    public void setOptionCPct(float optionCPct) { this.optionCPct = optionCPct; }

    public float getOptionDPct() { return optionDPct; }
    public void setOptionDPct(float optionDPct) { this.optionDPct = optionDPct; }

    public int getAttemptsCount() { return attemptsCount; }
    public void setAttemptsCount(int attemptsCount) { this.attemptsCount = attemptsCount; }
}
