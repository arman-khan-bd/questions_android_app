package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;
import com.questions.govt.data.model.CustomExamRequest;
import com.questions.govt.data.model.ExamResult;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.ReportItem;
import com.questions.govt.data.model.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result payload from GET /api/v1/profile/{username}.
 */
public class UserProfileResponse implements Serializable {

    @SerializedName(value = "user", alternate = {"student"})
    private User user;

    @SerializedName("global_rank")
    private int globalRank;

    @SerializedName("tab")
    private String tab;

    @SerializedName("tab_data")
    private List<Question> tabData = new ArrayList<>();

    @SerializedName("my_exams")
    private List<ExamResult> myExams = new ArrayList<>();

    @SerializedName("created_questions")
    private List<Question> createdQuestions = new ArrayList<>();

    @SerializedName("custom_exams")
    private List<CustomExamRequest> customExams = new ArrayList<>();

    @SerializedName("starred_questions")
    private List<Question> starredQuestions = new ArrayList<>();

    @SerializedName("reported_questions")
    private List<ReportItem> reportedQuestions = new ArrayList<>();

    @SerializedName("mistake_notebook")
    private List<Question> mistakeNotebook = new ArrayList<>();

    public User getUser() {
        if (user != null && globalRank > 0 && user.getGlobalRank() == 0) {
            user.setGlobalRank(globalRank);
        }
        return user;
    }
    public void setUser(User user) { this.user = user; }

    public int getGlobalRank() { return globalRank; }
    public void setGlobalRank(int globalRank) { this.globalRank = globalRank; }

    public String getTab() { return tab; }
    public void setTab(String tab) { this.tab = tab; }

    public List<ExamResult> getMyExams() { return myExams != null ? myExams : new ArrayList<>(); }
    public void setMyExams(List<ExamResult> myExams) { this.myExams = myExams; }

    public List<Question> getCreatedQuestions() {
        if (createdQuestions != null && !createdQuestions.isEmpty()) return createdQuestions;
        if ("questions".equals(tab) && tabData != null) return tabData;
        return new ArrayList<>();
    }
    public void setCreatedQuestions(List<Question> createdQuestions) { this.createdQuestions = createdQuestions; }

    public List<CustomExamRequest> getCustomExams() { return customExams != null ? customExams : new ArrayList<>(); }
    public void setCustomExams(List<CustomExamRequest> customExams) { this.customExams = customExams; }

    public List<Question> getStarredQuestions() {
        if (starredQuestions != null && !starredQuestions.isEmpty()) return starredQuestions;
        if ("stars".equals(tab) && tabData != null) return tabData;
        return new ArrayList<>();
    }
    public void setStarredQuestions(List<Question> starredQuestions) { this.starredQuestions = starredQuestions; }

    public List<ReportItem> getReportedQuestions() { return reportedQuestions != null ? reportedQuestions : new ArrayList<>(); }
    public void setReportedQuestions(List<ReportItem> reportedQuestions) { this.reportedQuestions = reportedQuestions; }

    public List<Question> getMistakeNotebook() {
        if (mistakeNotebook != null && !mistakeNotebook.isEmpty()) return mistakeNotebook;
        if ("mistakes".equals(tab) && tabData != null) return tabData;
        return new ArrayList<>();
    }
    public void setMistakeNotebook(List<Question> mistakeNotebook) { this.mistakeNotebook = mistakeNotebook; }
}
