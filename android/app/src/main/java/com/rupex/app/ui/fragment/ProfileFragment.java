package com.rupex.app.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rupex.app.R;
import com.rupex.app.sync.SyncManager;
import com.rupex.app.ui.LoginActivity;
import com.rupex.app.ui.MainViewModel;
import com.rupex.app.util.TokenManager;

/**
 * Fragment showing user profile and settings
 */
public class ProfileFragment extends Fragment {

    private MainViewModel viewModel;
    private TokenManager tokenManager;
    
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvTotalTransactions;
    private TextView tvAccountCount;
    private TextView tvSmsCount;
    private TextView tvSmsStatus;
    private LinearLayout optionSmsPermission;
    private LinearLayout optionSync;
    private LinearLayout optionLogout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tokenManager = TokenManager.getInstance(requireContext());
        
        // Initialize views
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvTotalTransactions = view.findViewById(R.id.tvTotalTransactions);
        tvAccountCount = view.findViewById(R.id.tvAccountCount);
        tvSmsCount = view.findViewById(R.id.tvSmsCount);
        tvSmsStatus = view.findViewById(R.id.tvSmsStatus);
        optionSmsPermission = view.findViewById(R.id.optionSmsPermission);
        optionSync = view.findViewById(R.id.optionSync);
        optionLogout = view.findViewById(R.id.optionLogout);

        setupUserInfo();
        setupOptions();
        setupViewModel();
    }

    private void setupUserInfo() {
        // Get user info from token manager or show defaults
        String email = tokenManager.getUserEmail();
        if (email != null) {
            tvUserEmail.setText(email);
            // Extract name from email
            String name = email.split("@")[0];
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            tvUserName.setText(name);
        }
    }

    private void setupOptions() {
        // SMS Permission
        optionSmsPermission.setOnClickListener(v -> {
            // Show permission status
            if (hasSmsPermissions()) {
                Toast.makeText(requireContext(), "SMS permission already granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Please grant SMS permission in settings", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Update SMS status
        updateSmsStatus();

        // Force sync
        optionSync.setOnClickListener(v -> {
            SyncManager.scheduleSyncNow(requireContext());
            Toast.makeText(requireContext(), "Sync started...", Toast.LENGTH_SHORT).show();
        });

        // Logout
        optionLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Observe transaction count
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                tvTotalTransactions.setText(String.valueOf(transactions.size()));
            }
        });

        // Observe accounts
        viewModel.getAccounts().observe(getViewLifecycleOwner(), accounts -> {
            if (accounts != null) {
                tvAccountCount.setText(String.valueOf(accounts.size()));
            }
        });

        // Observe pending count (SMS parsed)
        viewModel.getPendingCount().observe(getViewLifecycleOwner(), count -> {
            tvSmsCount.setText(String.valueOf(count != null ? count : 0));
        });
    }

    private boolean hasSmsPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) 
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void updateSmsStatus() {
        if (hasSmsPermissions()) {
            tvSmsStatus.setText("Granted");
            tvSmsStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.income));
        } else {
            tvSmsStatus.setText("Not Granted");
            tvSmsStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense));
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        tokenManager.clearTokens();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSmsStatus();
        viewModel.loadTransactions();
        viewModel.loadAccounts();
    }
}
