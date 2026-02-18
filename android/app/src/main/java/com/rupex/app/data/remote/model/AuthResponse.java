package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * Auth response - specialized for the flat auth response structure
 */
public class AuthResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("error")
    public String error;

    @SerializedName("user")
    private UserDto user;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getError() { return error; }
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

        @SerializedName("currency")
        private String currency;

        @SerializedName("timezone")
        private String timezone;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getCurrency() { return currency; }
        public String getTimezone() { return timezone; }
    }
}
