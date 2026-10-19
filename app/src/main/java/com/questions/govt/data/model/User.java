package com.questions.govt.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Represents an authenticated or public student user in the Govt Exam Prep platform.
 */
@Entity(tableName = "users")
public class User implements Serializable {

    @PrimaryKey
    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("username")
    private String username;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("cover_photo_url")
    private String coverPhotoUrl;

    @SerializedName("bio")
    private String bio;

    @SerializedName("school")
    private String school;

    @SerializedName("college")
    private String college;

    @SerializedName("university")
    private String university;

    @SerializedName("graduation_year")
    private String graduationYear;

    @SerializedName("current_city")
    private String currentCity;

    @SerializedName("hometown")
    private String hometown;

    @SerializedName("target_exam")
    private String targetExam;

    @SerializedName(value = "account_status", alternate = {"status"})
    private String accountStatus; // "active", "suspended", "disabled"

    @SerializedName("role")
    private String role;

    @SerializedName("is_suspended")
    private boolean isSuspended;

    @SerializedName("suspension_reason")
    private String suspensionReason;

    @SerializedName(value = "suspension_expires_at", alternate = {"suspended_until"})
    private String suspensionExpiresAt;

    @SerializedName("global_rank")
    private int globalRank;

    @SerializedName(value = "total_points", alternate = {"rank_points"})
    private double totalPoints;

    @SerializedName("easy_points")
    private double easyPoints;

    @SerializedName("medium_points")
    private double mediumPoints;

    @SerializedName("hard_points")
    private double hardPoints;

    @SerializedName("accuracy_rate")
    private double accuracyRate;

    @SerializedName("study_streak_days")
    private int studyStreakDays;

    @SerializedName("education_level")
    private String educationLevel;

    @SerializedName("tier_badge")
    private String tierBadge;

    @SerializedName("is_phone_private")
    private boolean isPhonePrivate;

    @SerializedName("is_email_private")
    private boolean isEmailPrivate;

    @SerializedName("created_questions_count")
    private int createdQuestionsCount;

    @SerializedName("attended_exams_count")
    private int attendedExamsCount;

    public User() {
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() {
        return resolveMediaUrl(avatarUrl);
    }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCoverPhotoUrl() {
        return resolveMediaUrl(coverPhotoUrl);
    }
    public void setCoverPhotoUrl(String coverPhotoUrl) { this.coverPhotoUrl = coverPhotoUrl; }

    private String resolveMediaUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        url = url.trim();
        if (url.startsWith("http://localhost:") || url.startsWith("http://localhost/") || url.startsWith("https://localhost:") || url.startsWith("https://localhost/")) {
            url = url.replaceFirst("^https?://localhost(:[0-9]+)?/?", com.questions.govt.AppConfig.SERVER_URL);
        } else if (url.startsWith("http://127.0.0.1:") || url.startsWith("http://127.0.0.1/") || url.startsWith("https://127.0.0.1:") || url.startsWith("https://127.0.0.1/")) {
            url = url.replaceFirst("^https?://127\\.0\\.0\\.1(:[0-9]+)?/?", com.questions.govt.AppConfig.SERVER_URL);
        } else if (url.startsWith("http://10.0.2.2:") || url.startsWith("http://10.0.2.2/") || url.startsWith("https://10.0.2.2:") || url.startsWith("https://10.0.2.2/")) {
            url = url.replaceFirst("^https?://10\\.0\\.2\\.2(:[0-9]+)?/?", com.questions.govt.AppConfig.SERVER_URL);
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.startsWith("/")) {
                url = url.substring(1);
            }
            url = com.questions.govt.AppConfig.SERVER_URL + url;
        }
        return url;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getGraduationYear() { return graduationYear; }
    public void setGraduationYear(String graduationYear) { this.graduationYear = graduationYear; }

    public String getCurrentCity() { return currentCity; }
    public void setCurrentCity(String currentCity) { this.currentCity = currentCity; }

    public String getHometown() { return hometown; }
    public void setHometown(String hometown) { this.hometown = hometown; }

    public String getTargetExam() { return targetExam; }
    public void setTargetExam(String targetExam) { this.targetExam = targetExam; }

    public String getAccountStatus() { return accountStatus != null ? accountStatus : "active"; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String suspensionReason) { this.suspensionReason = suspensionReason; }

    public String getSuspensionExpiresAt() { return suspensionExpiresAt; }
    public void setSuspensionExpiresAt(String suspensionExpiresAt) { this.suspensionExpiresAt = suspensionExpiresAt; }

    public int getGlobalRank() { return globalRank; }
    public void setGlobalRank(int globalRank) { this.globalRank = globalRank; }

    public double getTotalPoints() { return totalPoints; }
    public void setTotalPoints(double totalPoints) { this.totalPoints = totalPoints; }

    public String getTierBadge() { return tierBadge != null ? tierBadge : "Novice Aspirant"; }
    public void setTierBadge(String tierBadge) { this.tierBadge = tierBadge; }

    public boolean isPhonePrivate() { return isPhonePrivate; }
    public void setPhonePrivate(boolean phonePrivate) { isPhonePrivate = phonePrivate; }

    public boolean isEmailPrivate() { return isEmailPrivate; }
    public void setEmailPrivate(boolean emailPrivate) { isEmailPrivate = emailPrivate; }

    public int getCreatedQuestionsCount() { return createdQuestionsCount; }
    public void setCreatedQuestionsCount(int count) { this.createdQuestionsCount = count; }

    public int getAttendedExamsCount() { return attendedExamsCount; }
    public void setAttendedExamsCount(int count) { this.attendedExamsCount = count; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isSuspended() { return isSuspended; }
    public void setSuspended(boolean suspended) { isSuspended = suspended; }

    public double getEasyPoints() { return easyPoints; }
    public void setEasyPoints(double easyPoints) { this.easyPoints = easyPoints; }

    public double getMediumPoints() { return mediumPoints; }
    public void setMediumPoints(double mediumPoints) { this.mediumPoints = mediumPoints; }

    public double getHardPoints() { return hardPoints; }
    public void setHardPoints(double hardPoints) { this.hardPoints = hardPoints; }

    public double getAccuracyRate() { return accuracyRate; }
    public void setAccuracyRate(double accuracyRate) { this.accuracyRate = accuracyRate; }

    public int getStudyStreakDays() { return studyStreakDays; }
    public void setStudyStreakDays(int studyStreakDays) { this.studyStreakDays = studyStreakDays; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
}
