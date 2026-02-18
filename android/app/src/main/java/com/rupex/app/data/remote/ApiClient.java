package com.rupex.app.data.remote;

import android.content.Context;
import android.util.Log;

import com.rupex.app.BuildConfig;
import com.rupex.app.util.ServerUrlManager;
import com.rupex.app.util.TokenManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton API client using Retrofit
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static volatile ApiClient INSTANCE;
    
    private final RupexApi api;
    private final TokenManager tokenManager;

    private ApiClient(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
        
        // Logging interceptor (only in debug)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG 
                ? HttpLoggingInterceptor.Level.BODY 
                : HttpLoggingInterceptor.Level.NONE);

        // Auth interceptor - adds JWT to requests
        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            
            // Skip auth for login/register endpoints
            String path = original.url().encodedPath();
            if (path.contains("/auth/login") || path.contains("/auth/register")) {
                return chain.proceed(original);
            }
            
            String token = tokenManager.getAccessToken();
            if (token == null || token.isEmpty()) {
                return chain.proceed(original);
            }
            
            Request authenticated = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
            
            return chain.proceed(authenticated);
        };

        // Token refresh interceptor
        Interceptor tokenRefreshInterceptor = chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);
            
            // If 401 Unauthorized, try to refresh token
            if (response.code() == 401 && !request.url().encodedPath().contains("/auth/")) {
                response.close();
                
                // Try to refresh token
                if (refreshToken()) {
                    // Retry with new token
                    String newToken = tokenManager.getAccessToken();
                    Request newRequest = request.newBuilder()
                            .header("Authorization", "Bearer " + newToken)
                            .build();
                    return chain.proceed(newRequest);
                }
            }
            
            return response;
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(tokenRefreshInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        String baseUrl = ServerUrlManager.getInstance(context).getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            // Fallback to BuildConfig if available, otherwise use a placeholder
            // This case should be handled by the UI redirecting to ServerSetupActivity
            baseUrl = BuildConfig.API_BASE_URL;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.api = retrofit.create(RupexApi.class);
    }

    /**
     * Reset the singleton instance (e.g. when server URL changes)
     */
    public static void resetInstance() {
        INSTANCE = null;
    }

    /**
     * Get singleton instance
     */
    public static ApiClient getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ApiClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ApiClient(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Get API interface
     */
    public RupexApi getApi() {
        return api;
    }

    /**
     * Try to refresh the access token
     */
    private boolean refreshToken() {
        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.w(TAG, "No refresh token available");
            return false;
        }

        try {
            retrofit2.Response<com.rupex.app.data.remote.model.AuthResponse> response =
                    api.refreshToken(new RupexApi.RefreshTokenRequest(refreshToken)).execute();

            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                com.rupex.app.data.remote.model.AuthResponse auth = response.body();
                tokenManager.saveTokens(auth.getAccessToken(), auth.getRefreshToken());
                Log.i(TAG, "Token refreshed successfully");
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to refresh token", e);
        }

        // Clear tokens on failure
        tokenManager.clearTokens();
        return false;
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return tokenManager.getAccessToken() != null;
    }

    /**
     * Clear auth state (logout)
     */
    public void logout() {
        tokenManager.clearTokens();
    }
}
