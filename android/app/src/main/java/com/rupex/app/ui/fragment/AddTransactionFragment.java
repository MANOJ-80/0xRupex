package com.rupex.app.ui.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rupex.app.R;
import com.rupex.app.data.model.Account;
import com.rupex.app.data.model.Category;
import com.rupex.app.ui.MainViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for adding manual transactions
 */
public class AddTransactionFragment extends Fragment {

    private MainViewModel viewModel;
    
    private MaterialButtonToggleGroup toggleType;
    private MaterialButton btnExpense;
    private MaterialButton btnIncome;
    private TextInputEditText etAmount;
    private AutoCompleteTextView actvCategory;
    private AutoCompleteTextView actvAccount;
    private TextInputEditText etDescription;
    private TextInputEditText etMerchant;
    private TextInputEditText etDate;
    private TextInputEditText etNotes;
    private MaterialButton btnSave;
    
    private Calendar selectedDate;
    private List<Category> categories = new ArrayList<>();
    private List<Account> accounts = new ArrayList<>();
    private Category selectedCategory;
    private Account selectedAccount;
    private String transactionType = "expense";
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        selectedDate = Calendar.getInstance();
        
        // Initialize views
        toggleType = view.findViewById(R.id.toggleType);
        btnExpense = view.findViewById(R.id.btnExpense);
        btnIncome = view.findViewById(R.id.btnIncome);
        etAmount = view.findViewById(R.id.etAmount);
        actvCategory = view.findViewById(R.id.actvCategory);
        actvAccount = view.findViewById(R.id.actvAccount);
        etDescription = view.findViewById(R.id.etDescription);
        etMerchant = view.findViewById(R.id.etMerchant);
        etDate = view.findViewById(R.id.etDate);
        etNotes = view.findViewById(R.id.etNotes);
        btnSave = view.findViewById(R.id.btnSave);

        setupTypeToggle();
        setupDatePicker();
        setupViewModel();
        setupSaveButton();
        
        // Set default date
        etDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void setupTypeToggle() {
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnExpense) {
                    transactionType = "expense";
                    loadCategoriesForType("expense");
                } else if (checkedId == R.id.btnIncome) {
                    transactionType = "income";
                    loadCategoriesForType("income");
                }
            }
        });
        
        // Default to expense
        btnExpense.setChecked(true);
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        etDate.setText(dateFormat.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Observe categories
        viewModel.getCategories().observe(getViewLifecycleOwner(), categoryList -> {
            if (categoryList != null) {
                this.categories = categoryList;
                loadCategoriesForType(transactionType);
            }
        });

        // Observe accounts
        viewModel.getAccounts().observe(getViewLifecycleOwner(), accountList -> {
            if (accountList != null) {
                this.accounts = accountList;
                updateAccountsDropdown();
            }
        });

        // Load data
        viewModel.loadCategories();
        viewModel.loadAccounts();
    }

    private void loadCategoriesForType(String type) {
        List<String> categoryNames = new ArrayList<>();
        for (Category cat : categories) {
            if (cat.getType().equals(type)) {
                categoryNames.add(cat.getName());
            }
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
        );
        actvCategory.setAdapter(adapter);
        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = categoryNames.get(position);
            for (Category cat : categories) {
                if (cat.getName().equals(selected)) {
                    selectedCategory = cat;
                    break;
                }
            }
        });
    }

    private void updateAccountsDropdown() {
        List<String> accountNames = new ArrayList<>();
        for (Account acc : accounts) {
            accountNames.add(acc.getName() + " (****" + acc.getAccountNumber() + ")");
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                accountNames
        );
        actvAccount.setAdapter(adapter);
        actvAccount.setOnItemClickListener((parent, view, position, id) -> {
            selectedAccount = accounts.get(position);
        });
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                saveTransaction();
            }
        });
    }

    private boolean validateInput() {
        String amountStr = etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Amount is required");
            return false;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            return false;
        }

        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return false;
        }

        String description = etDescription.getText().toString().trim();
        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            return false;
        }

        return true;
    }

    private void saveTransaction() {
        double amount = Double.parseDouble(etAmount.getText().toString().trim());
        String description = etDescription.getText().toString().trim();
        String merchant = etMerchant.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        viewModel.createTransaction(
                amount,
                transactionType,
                description,
                merchant,
                selectedCategory.getId(),
                selectedAccount != null ? selectedAccount.getId() : null,
                selectedDate.getTime(),
                notes
        );

        Toast.makeText(requireContext(), "Transaction saved!", Toast.LENGTH_SHORT).show();
        clearForm();
    }

    private void clearForm() {
        etAmount.setText("");
        actvCategory.setText("");
        actvAccount.setText("");
        etDescription.setText("");
        etMerchant.setText("");
        etNotes.setText("");
        selectedCategory = null;
        selectedAccount = null;
        selectedDate = Calendar.getInstance();
        etDate.setText(dateFormat.format(selectedDate.getTime()));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadCategories();
        viewModel.loadAccounts();
    }
}
