package com.rupex.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.rupex.app.R;
import com.rupex.app.data.remote.ApiClient;
import com.rupex.app.util.ServerUrlManager;
import com.rupex.app.util.TokenManager;

public class ServerSetupActivity extends AppCompatActivity {

    private TextInputEditText etServerUrl;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_setup);

        etServerUrl = findViewById(R.id.etServerUrl);
        btnConnect = findViewById(R.id.btnConnect);

        btnConnect.setOnClickListener(v -> {
            String url = etServerUrl.getText().toString().trim();
            if (validateUrl(url)) {
                saveUrlAndProceed(url);
            }
        });
    }

    private boolean validateUrl(String url) {
        if (url.isEmpty()) {
            etServerUrl.setError("URL cannot be empty");
            return false;
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            etServerUrl.setError("Invalid URL format");
            return false;
        }
        return true;
    }

    private void saveUrlAndProceed(String url) {
        // Ensure it ends with /api/v1/
        // Logic: 
        // 1. Remove trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        // 2. Check if it already has /api/v1
        if (!url.endsWith("/api/v1")) {
            url += "/api/v1";
        }
        
        // 3. Add trailing slash
        url += "/";

        ServerUrlManager.getInstance(this).saveBaseUrl(url);
        
        // Clear any existing tokens as we are switching servers
        TokenManager.getInstance(this).clearTokens();
        
        // Reset ApiClient to use new URL
        ApiClient.resetInstance();

        Toast.makeText(this, "Server URL saved", Toast.LENGTH_SHORT).show();

        // Navigate to Login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
