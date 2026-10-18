package com.govtexam.prep.data.remote.request;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Request payload for POST /api/v1/auth/login.
 */
public class LoginRequest implements Serializable {

    @SerializedName("identifier")
    private String identifier; // Phone or Email

    @SerializedName("password")
    private String password;

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
