package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * Auth response with tokens
 */
public class AuthResponse {
    
    @SerializedName("user")
    private UserDto user;
    
    @SerializedName("accessToken")
    private String accessToken;
    
    @SerializedName("refreshToken")
    private String refreshToken;

    public UserDto getUser() { return user; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }

    public static class UserDto {
        @SerializedName("id")
        private String id;
        
        @SerializedName("email")
        private String email;
        
        @SerializedName("name")
        private String name;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
    }
}
