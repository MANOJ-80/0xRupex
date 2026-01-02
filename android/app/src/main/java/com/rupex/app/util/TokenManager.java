package com.rupex.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Secure token storage using EncryptedSharedPreferences
 */
public class TokenManager {

    private static final String TAG = "TokenManager";
    private static final String PREFS_NAME = "rupex_secure_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";

    private static volatile TokenManager INSTANCE;
    private final SharedPreferences prefs;

    private TokenManager(Context context) {
        SharedPreferences encryptedPrefs = null;
        
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to create encrypted prefs, falling back to regular", e);
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        
        this.prefs = encryptedPrefs;
    }

    public static TokenManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TokenManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TokenManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Save auth tokens
     */
    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    /**
     * Save user info
     */
    public void saveUser(String userId, String email, String name) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_NAME, name)
                .apply();
    }

    /**
     * Get access token
     */
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Get refresh token
     */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * Get user ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /**
     * Get user email
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Get user name
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    /**
     * Check if logged in
     */
    public boolean isLoggedIn() {
        return getAccessToken() != null && !getAccessToken().isEmpty();
    }

    /**
     * Clear all tokens (logout)
     */
    public void clearTokens() {
        prefs.edit().clear().apply();
    }
}
