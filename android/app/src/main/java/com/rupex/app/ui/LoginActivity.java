package com.rupex.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rupex.app.databinding.ActivityLoginBinding;
import com.rupex.app.data.remote.ApiClient;
import com.rupex.app.data.remote.model.ApiResponse;
import com.rupex.app.data.remote.model.AuthResponse;
import com.rupex.app.data.remote.model.LoginRequest;
import com.rupex.app.util.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Login/Register activity
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private TokenManager tokenManager;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = TokenManager.getInstance(this);

        // Check if already logged in
        if (tokenManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        setupUI();
    }

    private void setupUI() {
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode) {
                login(email, password);
            } else {
                register(email, password);
            }
        });

        binding.textToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });
    }

    private void updateUI() {
        if (isLoginMode) {
            binding.btnLogin.setText("Login");
            binding.textToggleMode.setText("Don't have an account? Register");
            binding.textTitle.setText("Welcome Back");
        } else {
            binding.btnLogin.setText("Register");
            binding.textToggleMode.setText("Already have an account? Login");
            binding.textTitle.setText("Create Account");
        }
    }

    private void login(String email, String password) {
        showLoading(true);

        ApiClient.getInstance(this).getApi()
                .login(new LoginRequest(email, password))
                .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse>> call, 
                                         Response<ApiResponse<AuthResponse>> response) {
                        showLoading(false);
                        handleAuthResponse(response);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, 
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void register(String email, String password) {
        showLoading(true);

        ApiClient.getInstance(this).getApi()
                .register(new LoginRequest(email, password))
                .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse>> call, 
                                         Response<ApiResponse<AuthResponse>> response) {
                        showLoading(false);
                        handleAuthResponse(response);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, 
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleAuthResponse(Response<ApiResponse<AuthResponse>> response) {
        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            AuthResponse auth = response.body().getData();
            
            // Save tokens
            tokenManager.saveTokens(auth.getAccessToken(), auth.getRefreshToken());
            tokenManager.saveUser(
                    auth.getUser().getId(),
                    auth.getUser().getEmail(),
                    auth.getUser().getName()
            );

            Toast.makeText(this, "Welcome, " + auth.getUser().getName(), Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            String error = response.body() != null ? response.body().getError() : "Authentication failed";
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
