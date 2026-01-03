package com.rupex.app.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.rupex.app.R;
import com.rupex.app.data.local.entity.PendingTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Dialog for editing transaction category, type, and note
 */
public class EditTransactionDialog extends DialogFragment {

    public interface OnTransactionEditedListener {
        void onTransactionEdited(long transactionId, String newCategory, String newType, String note, Double amount, String merchant, Long transactionAt);
    }

    private static final String ARG_TRANSACTION_ID = "transaction_id";
    private static final String ARG_MERCHANT = "merchant";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_CURRENT_CATEGORY = "current_category";
    private static final String ARG_CURRENT_TYPE = "current_type";
    private static final String ARG_CURRENT_NOTE = "current_note";
    private static final String ARG_TRANSACTION_DATE = "transaction_date";
    
    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    private OnTransactionEditedListener listener;
    private CategoryAdapter categoryAdapter;
    private String selectedCategory;
    private String selectedType;
    private long transactionId;
    private EditText etNote;
    private EditText etMerchant;
    private EditText etAmount;
    private TextView tvDateTime;
    private double originalAmount;
    private Calendar selectedDateTime;
    private long originalDateTime;

    // Pre-defined categories with icons
    private static final CategoryItem[] CATEGORIES = {
            new CategoryItem("Food & Dining", "🍕", "#FF5722"),
            new CategoryItem("Groceries", "🛒", "#4CAF50"),
            new CategoryItem("Shopping", "🛍️", "#E91E63"),
            new CategoryItem("Transport", "🚗", "#2196F3"),
            new CategoryItem("Entertainment", "🎬", "#9C27B0"),
            new CategoryItem("Bills & Utilities", "💡", "#FF9800"),
            new CategoryItem("Health", "💊", "#00BCD4"),
            new CategoryItem("Education", "📚", "#3F51B5"),
            new CategoryItem("Travel", "✈️", "#009688"),
            new CategoryItem("Personal Care", "💇", "#F44336"),
            new CategoryItem("Friends & Family", "👥", "#673AB7"),
            new CategoryItem("Salary", "💰", "#4CAF50"),
            new CategoryItem("Investment", "📈", "#8BC34A"),
            new CategoryItem("Refund", "↩️", "#03A9F4"),
            new CategoryItem("Other", "📦", "#607D8B")
    };

    public static EditTransactionDialog newInstance(PendingTransaction txn) {
        EditTransactionDialog dialog = new EditTransactionDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_TRANSACTION_ID, txn.getId());
        args.putString(ARG_MERCHANT, txn.getMerchant());
        args.putDouble(ARG_AMOUNT, txn.getAmount());
        args.putString(ARG_CURRENT_CATEGORY, txn.getCategory());
        args.putString(ARG_CURRENT_TYPE, txn.getType());
        args.putString(ARG_CURRENT_NOTE, txn.getNote());
        args.putLong(ARG_TRANSACTION_DATE, txn.getTransactionAt());
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnTransactionEditedListener) {
            listener = (OnTransactionEditedListener) getParentFragment();
        } else if (context instanceof OnTransactionEditedListener) {
            listener = (OnTransactionEditedListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_Rupex_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_edit_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }

        transactionId = args.getLong(ARG_TRANSACTION_ID);
        String merchant = args.getString(ARG_MERCHANT, "Transaction");
        double amount = args.getDouble(ARG_AMOUNT, 0);
        selectedCategory = args.getString(ARG_CURRENT_CATEGORY, "Other");
        selectedType = args.getString(ARG_CURRENT_TYPE, "expense");
        String currentNote = args.getString(ARG_CURRENT_NOTE, "");
        long transactionDate = args.getLong(ARG_TRANSACTION_DATE, System.currentTimeMillis());

        // Setup views
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        etMerchant = view.findViewById(R.id.etMerchant);
        etAmount = view.findViewById(R.id.etAmount);
        tvDateTime = view.findViewById(R.id.tvDateTime);
        etNote = view.findViewById(R.id.etNote);
        RadioGroup rgType = view.findViewById(R.id.rgTransactionType);
        RadioButton rbExpense = view.findViewById(R.id.rbExpense);
        RadioButton rbIncome = view.findViewById(R.id.rbIncome);
        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        // Initialize date/time
        originalDateTime = transactionDate;
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTimeInMillis(transactionDate);

        // Set current values
        tvTitle.setText("Edit Transaction");
        etMerchant.setText(merchant != null ? merchant : "");
        etAmount.setText(String.format(Locale.US, "%.2f", amount));
        originalAmount = amount;
        updateDateTimeDisplay();
        
        // Make date/time clickable
        tvDateTime.setOnClickListener(v -> showDatePicker());
        
        // Set current note
        if (currentNote != null && !currentNote.isEmpty()) {
            etNote.setText(currentNote);
        }

        // Set type
        if ("income".equals(selectedType)) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            selectedType = checkedId == R.id.rbIncome ? "income" : "expense";
        });

        // Setup category grid
        List<CategoryItem> categoryList = new ArrayList<>();
        for (CategoryItem cat : CATEGORIES) {
            categoryList.add(cat);
        }
        
        categoryAdapter = new CategoryAdapter(categoryList, selectedCategory, category -> {
            selectedCategory = category;
        });
        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rvCategories.setAdapter(categoryAdapter);

        // Buttons
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> {
            if (listener != null) {
                String note = etNote.getText().toString().trim();
                String newMerchant = etMerchant.getText().toString().trim();
                Double newAmount = null;
                try {
                    double parsedAmount = Double.parseDouble(etAmount.getText().toString().trim());
                    if (Math.abs(parsedAmount - originalAmount) > 0.001) {
                        newAmount = parsedAmount;
                    }
                } catch (NumberFormatException e) {
                    // Keep original amount if parsing fails
                }
                // Check if date/time changed
                Long newTransactionAt = null;
                if (selectedDateTime.getTimeInMillis() != originalDateTime) {
                    newTransactionAt = selectedDateTime.getTimeInMillis();
                }
                listener.onTransactionEdited(transactionId, selectedCategory, selectedType, note, newAmount, newMerchant, newTransactionAt);
            }
            dismiss();
        });
    }
    
    private void updateDateTimeDisplay() {
        tvDateTime.setText(DATE_FORMAT.format(selectedDateTime.getTime()) + " ✏️");
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePicker();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    // ==================== Category Item ====================

    public static class CategoryItem {
        public final String name;
        public final String icon;
        public final String color;

        public CategoryItem(String name, String icon, String color) {
            this.name = name;
            this.icon = icon;
            this.color = color;
        }
    }

    // ==================== Category Adapter ====================

    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

        public interface OnCategorySelectedListener {
            void onCategorySelected(String category);
        }

        private final List<CategoryItem> categories;
        private String selectedCategory;
        private final OnCategorySelectedListener listener;

        public CategoryAdapter(List<CategoryItem> categories, String selected, OnCategorySelectedListener listener) {
            this.categories = categories;
            this.selectedCategory = selected;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_picker, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryItem item = categories.get(position);
            holder.tvIcon.setText(item.icon);
            holder.tvName.setText(item.name);

            boolean isSelected = item.name.equals(selectedCategory);
            holder.itemView.setSelected(isSelected);
            holder.itemView.setAlpha(isSelected ? 1.0f : 0.6f);

            holder.itemView.setOnClickListener(v -> {
                String oldSelection = selectedCategory;
                selectedCategory = item.name;
                
                // Update old and new selection
                int oldPos = findPosition(oldSelection);
                if (oldPos >= 0) notifyItemChanged(oldPos);
                notifyItemChanged(position);
                
                if (listener != null) {
                    listener.onCategorySelected(item.name);
                }
            });
        }

        private int findPosition(String category) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).name.equals(category)) return i;
            }
            return -1;
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon;
            TextView tvName;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
                tvName = itemView.findViewById(R.id.tvCategoryName);
            }
        }
    }
}
