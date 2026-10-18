package com.questions.govt.data.remote.request;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Request payload for POST /api/v1/auth/register.
 */
public class RegisterRequest implements Serializable {

    @SerializedName("name")
    private String name;

    @SerializedName("identifier")
    private String identifier; // Phone number OR email address

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("password")
    private String password;

    @SerializedName("password_confirmation")
    private String passwordConfirmation;

    public RegisterRequest(String name, String identifier, String password) {
        this.name = name;
        this.identifier = identifier != null ? identifier.trim() : "";
        if (this.identifier.contains("@")) {
            this.email = this.identifier;
            this.phone = null;
        } else {
            this.phone = this.identifier;
            this.email = null;
        }
        this.password = password;
        this.passwordConfirmation = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordConfirmation() { return passwordConfirmation; }
    public void setPasswordConfirmation(String passwordConfirmation) { this.passwordConfirmation = passwordConfirmation; }
}
