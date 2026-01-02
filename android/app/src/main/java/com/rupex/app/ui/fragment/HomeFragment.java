package com.rupex.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rupex.app.R;
import com.rupex.app.data.local.entity.PendingTransaction;
import com.rupex.app.ui.EditTransactionDialog;
import com.rupex.app.ui.TransactionAdapter;
import com.rupex.app.ui.MainViewModel;

/**
 * Home fragment showing balance summary and recent transactions
 */
public class HomeFragment extends Fragment implements EditTransactionDialog.OnTransactionEditedListener {

    private MainViewModel viewModel;
    private TransactionAdapter adapter;
    
    private TextView tvTotalBalance;
    private TextView tvIncome;
    private TextView tvExpense;
    private RecyclerView recyclerRecent;
    private View layoutEmpty;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        recyclerRecent = view.findViewById(R.id.rvRecentTransactions);
        layoutEmpty = view.findViewById(R.id.emptyState);

        setupRecyclerView();
        setupViewModel();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        adapter.setOnTransactionClickListener(this::showEditDialog);
        recyclerRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRecent.setAdapter(adapter);
        recyclerRecent.setNestedScrollingEnabled(false);
    }

    private void showEditDialog(PendingTransaction transaction) {
        EditTransactionDialog dialog = EditTransactionDialog.newInstance(transaction);
        dialog.show(getChildFragmentManager(), "edit_transaction");
    }

    @Override
    public void onTransactionEdited(long transactionId, String newCategory, String newType, String note) {
        viewModel.updateTransaction(transactionId, newCategory, newType, note);
        Toast.makeText(requireContext(), "Transaction updated", Toast.LENGTH_SHORT).show();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Observe transactions (show only recent 5)
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                // Show only last 5 transactions
                int count = Math.min(transactions.size(), 5);
                adapter.submitList(transactions.subList(0, count));
                recyclerRecent.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                recyclerRecent.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        // Observe total balance
        viewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                tvTotalBalance.setText(String.format("₹%.2f", balance));
            }
        });

        // Observe income (from local DB)
        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> {
            if (income != null) {
                tvIncome.setText(String.format("₹%.0f", income));
            }
        });

        // Observe expense (from local DB)
        viewModel.getTotalExpense().observe(getViewLifecycleOwner(), expense -> {
            if (expense != null) {
                tvExpense.setText(String.format("₹%.0f", expense));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadTransactions();
    }
}
