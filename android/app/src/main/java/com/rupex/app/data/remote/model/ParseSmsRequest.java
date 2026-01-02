package com.rupex.app.data.remote.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request to parse SMS on server
 */
public class ParseSmsRequest {
    
    @SerializedName("smsBody")
    private String smsBody;
    
    @SerializedName("sender")
    private String sender;
    
    @SerializedName("timestamp")
    private String timestamp;

    public ParseSmsRequest(String sender, String smsBody, long timestamp) {
        this.sender = sender;
        this.smsBody = smsBody;
        this.timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 
                java.util.Locale.US).format(new java.util.Date(timestamp));
    }
}
