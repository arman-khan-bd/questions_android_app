package com.govtexam.prep.data.remote.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Result of upvoting/downvoting a question accuracy dispute.
 */
public class VoteResponse implements Serializable {

    @SerializedName("upvotes_count")
    private int upvotesCount;

    @SerializedName("downvotes_count")
    private int downvotesCount;

    @SerializedName("user_voted")
    private String userVoted; // "up", "down", or null

    public int getUpvotesCount() { return upvotesCount; }
    public void setUpvotesCount(int upvotesCount) { this.upvotesCount = upvotesCount; }

    public int getDownvotesCount() { return downvotesCount; }
    public void setDownvotesCount(int downvotesCount) { this.downvotesCount = downvotesCount; }

    public String getUserVoted() { return userVoted; }
    public void setUserVoted(String userVoted) { this.userVoted = userVoted; }
}
