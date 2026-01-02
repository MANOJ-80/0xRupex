package com.rupex.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.rupex.app.R;
import com.rupex.app.data.local.entity.PendingTransaction;
import com.rupex.app.ui.EditTransactionDialog;
import com.rupex.app.ui.GroupedTransactionAdapter;
import com.rupex.app.ui.MainViewModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Fragment showing full transaction history with date grouping
 */
public class TransactionsFragment extends Fragment implements EditTransactionDialog.OnTransactionEditedListener {

    private MainViewModel viewModel;
    private GroupedTransactionAdapter adapter;
    
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerTransactions;
    private ChipGroup chipFilter;
    private View layoutEmpty;
    
    private TextView tvMonth;
    private TextView tvSummaryIncome;
    private TextView tvSummaryExpenses;
    private TextView tvSummaryTotal;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    
    private Calendar currentMonth;
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
    private final NumberFormat currencyFormat;
    
    private String currentFilter = "all";

    public TransactionsFragment() {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setMaximumFractionDigits(2);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transactions_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        currentMonth = Calendar.getInstance();
        
        // Initialize views
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerTransactions = view.findViewById(R.id.rvTransactions);
        chipFilter = view.findViewById(R.id.chipGroup);
        layoutEmpty = view.findViewById(R.id.emptyState);
        
        tvMonth = view.findViewById(R.id.tvMonth);
        tvSummaryIncome = view.findViewById(R.id.tvSummaryIncome);
        tvSummaryExpenses = view.findViewById(R.id.tvSummaryExpenses);
        tvSummaryTotal = view.findViewById(R.id.tvSummaryTotal);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);

        setupRecyclerView();
        setupNavigation();
        setupFilters();
        setupViewModel();
        updateMonthDisplay();
    }

    private void setupRecyclerView() {
        adapter = new GroupedTransactionAdapter();
        adapter.setOnTransactionClickListener(this::showEditDialog);
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTransactions.setAdapter(adapter);
    }

    private void setupNavigation() {
        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            filterTransactionsByMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            if (currentMonth.before(now)) {
                currentMonth.add(Calendar.MONTH, 1);
                updateMonthDisplay();
                filterTransactionsByMonth();
            }
        });
    }
    
    private void updateMonthDisplay() {
        tvMonth.setText(monthFormat.format(currentMonth.getTime()));
    }
    
    private void filterTransactionsByMonth() {
        List<PendingTransaction> allTransactions = viewModel.getTransactions().getValue();
        if (allTransactions == null) return;
        
        // Get month bounds
        Calendar start = (Calendar) currentMonth.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        
        long startTime = start.getTimeInMillis();
        long endTime = end.getTimeInMillis();
        
        // Filter transactions
        List<PendingTransaction> filtered = new java.util.ArrayList<>();
        for (PendingTransaction txn : allTransactions) {
            long txnTime = txn.getTransactionAt();
            if (txnTime >= startTime && txnTime < endTime) {
                filtered.add(txn);
            }
        }
        
        if (!filtered.isEmpty()) {
            adapter.submitList(filtered);
            recyclerTransactions.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            updateSummary(filtered);
        } else {
            adapter.submitList(new java.util.ArrayList<>());
            recyclerTransactions.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvSummaryIncome.setText("₹0.00");
            tvSummaryExpenses.setText("₹0.00");
            tvSummaryTotal.setText("₹0.00");
        }
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

    private void setupFilters() {
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> {
            viewModel.syncTransactions();
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Observe transactions
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            swipeRefresh.setRefreshing(false);
            
            if (transactions != null && !transactions.isEmpty()) {
                // Filter by current month
                filterTransactionsByMonth();
            } else {
                recyclerTransactions.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                tvSummaryIncome.setText("₹0.00");
                tvSummaryExpenses.setText("₹0.00");
                tvSummaryTotal.setText("₹0.00");
            }
        });
    }
    
    private void updateSummary(List<PendingTransaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;
        
        for (PendingTransaction txn : transactions) {
            if ("income".equals(txn.getType())) {
                totalIncome += txn.getAmount();
            } else {
                totalExpense += txn.getAmount();
            }
        }
        
        double total = totalIncome - totalExpense;
        
        tvSummaryIncome.setText(currencyFormat.format(totalIncome));
        tvSummaryExpenses.setText(currencyFormat.format(totalExpense));
        tvSummaryTotal.setText(currencyFormat.format(total));
        
        // Color total based on positive/negative
        tvSummaryTotal.setTextColor(requireContext().getColor(
                total >= 0 ? R.color.text_primary : R.color.expense));
    }

    private void applyFilter() {
        viewModel.setTransactionFilter(currentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadTransactions();
    }
}
