package com.rupex.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public class ServerUrlManager {
    private static final String PREFS_NAME = "rupex_config_prefs";
    private static final String KEY_BASE_URL = "base_url";
    
    private static volatile ServerUrlManager INSTANCE;
    private final SharedPreferences prefs;

    private ServerUrlManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static ServerUrlManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ServerUrlManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServerUrlManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void saveBaseUrl(String url) {
        prefs.edit().putString(KEY_BASE_URL, url).apply();
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, null);
    }

    public boolean isUrlSet() {
        return getBaseUrl() != null;
    }
    
    public void clear() {
        prefs.edit().remove(KEY_BASE_URL).apply();
    }
}
