package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * API response wrapper
 */
public class ApiResponse<T> {
    
    @SerializedName("success")
    public boolean success;
    
    @SerializedName("data")
    public T data;
    
    @SerializedName("message")
    public String message;
    
    @SerializedName("error")
    public String error;

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public String getError() { return error; }
}
