package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Result of starring/unstarring a question.
 */
public class StarResponse implements Serializable {

    @SerializedName("is_starred")
    private boolean isStarred;

    @SerializedName("stars_count")
    private int starsCount;

    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { isStarred = starred; }

    public int getStarsCount() { return starsCount; }
    public void setStarsCount(int starsCount) { this.starsCount = starsCount; }
}
