package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * Login request payload
 */
public class LoginRequest {
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("password")
    private String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
