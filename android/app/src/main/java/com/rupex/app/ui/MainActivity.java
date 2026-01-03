package com.rupex.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rupex.app.R;
import com.rupex.app.databinding.ActivityMainNavBinding;
import com.rupex.app.sync.SyncManager;
import com.rupex.app.ui.fragment.AddTransactionFragment;
import com.rupex.app.ui.fragment.ChartsFragment;
import com.rupex.app.ui.fragment.HomeFragment;
import com.rupex.app.ui.fragment.ProfileFragment;
import com.rupex.app.ui.fragment.TransactionsFragment;
import com.rupex.app.util.TokenManager;

/**
 * Main activity with bottom navigation and fragments
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainNavBinding binding;
    private MainViewModel viewModel;
    private TokenManager tokenManager;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean smsGranted = result.getOrDefault(Manifest.permission.RECEIVE_SMS, false);
                Boolean readSmsGranted = result.getOrDefault(Manifest.permission.READ_SMS, false);
                
                if (Boolean.TRUE.equals(smsGranted) && Boolean.TRUE.equals(readSmsGranted)) {
                    Toast.makeText(this, "SMS permission granted! Auto-tracking enabled.", Toast.LENGTH_SHORT).show();
                    binding.cardSmsPermission.setVisibility(View.GONE);
                    SyncManager.schedulePeriodicSync(this);
                } else {
                    showPermissionRationale();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainNavBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = TokenManager.getInstance(this);
        
        // Check if server URL is set
        if (!com.rupex.app.util.ServerUrlManager.getInstance(this).isUrlSet()) {
            startActivity(new Intent(this, ServerSetupActivity.class));
            finish();
            return;
        }
        
        // Check if logged in
        if (!tokenManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupToolbar();
        setupViewModel();
        setupBottomNavigation();
        checkPermissions();
        
        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        
        // Grant SMS permission button
        binding.btnGrantSms.setOnClickListener(v -> {
            requestSmsPermissions();
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_transactions) {
                fragment = new TransactionsFragment();
            } else if (itemId == R.id.nav_add) {
                fragment = new AddTransactionFragment();
            } else if (itemId == R.id.nav_charts) {
                fragment = new ChartsFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }
            
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void checkPermissions() {
        if (!hasSmsPermissions()) {
            binding.cardSmsPermission.setVisibility(View.VISIBLE);
        } else {
            binding.cardSmsPermission.setVisibility(View.GONE);
            SyncManager.schedulePeriodicSync(this);
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
            }
        }
    }

    private boolean hasSmsPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) 
                == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermissions() {
        permissionLauncher.launch(new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
        });
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sms_permission_title)
                .setMessage(R.string.sms_permission_message)
                .setPositiveButton(R.string.grant_permission, (dialog, which) -> {
                    requestSmsPermissions();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenManager.isLoggedIn()) {
            viewModel.loadTransactions();
        }
    }
}
