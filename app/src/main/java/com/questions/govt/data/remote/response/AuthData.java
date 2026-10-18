package com.questions.govt.data.remote.response;

import com.google.gson.annotations.SerializedName;
import com.questions.govt.data.model.User;

import java.io.Serializable;

/**
 * Data payload returned by /api/v1/auth/login and /api/v1/auth/register.
 */
public class AuthData implements Serializable {

    @SerializedName("token")
    private String token;

    @SerializedName("token_type")
    private String tokenType = "Bearer";

    @SerializedName("user")
    private User user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
