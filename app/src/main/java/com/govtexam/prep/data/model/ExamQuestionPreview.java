package com.govtexam.prep.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Question model for post-exam review and answers preview.
 */
public class ExamQuestionPreview implements Serializable {

    @SerializedName("question_id")
    private long questionId;

    @SerializedName("question_text")
    private String questionText;

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

    @SerializedName("user_selected_option")
    private String userSelectedOption; // "A", "B", "C", "D", or null

    @SerializedName("is_correct")
    private boolean isCorrect;

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

    @SerializedName("is_starred")
    private boolean isStarred;

    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }

    public String getUserSelectedOption() { return userSelectedOption; }
    public void setUserSelectedOption(String userSelectedOption) { this.userSelectedOption = userSelectedOption; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

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

    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { isStarred = starred; }
}
