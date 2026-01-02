package com.rupex.app.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rupex.app.R;
import com.rupex.app.data.local.entity.PendingTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for transactions grouped by date
 */
public class GroupedTransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_TRANSACTION = 1;

    public interface OnTransactionClickListener {
        void onTransactionClick(PendingTransaction transaction);
    }

    // Category colors
    private static final Map<String, String> CATEGORY_COLORS = new HashMap<String, String>() {{
        put("food", "#FF5722");
        put("food & dining", "#FF5722");
        put("grocery", "#4CAF50");
        put("groceries", "#4CAF50");
        put("shopping", "#E91E63");
        put("transport", "#2196F3");
        put("transportation", "#2196F3");
        put("entertainment", "#9C27B0");
        put("bills", "#FF9800");
        put("bills & utilities", "#FF9800");
        put("utilities", "#FF9800");
        put("health", "#00BCD4");
        put("education", "#3F51B5");
        put("travel", "#009688");
        put("personal care", "#F44336");
        put("friends", "#673AB7");
        put("friends & family", "#673AB7");
        put("social life", "#673AB7");
        put("salary", "#4CAF50");
        put("investment", "#8BC34A");
        put("refund", "#03A9F4");
        put("household", "#795548");
        put("beauty", "#E91E63");
        put("other", "#607D8B");
        put("uncategorized", "#9E9E9E");
    }};

    private final List<Object> items = new ArrayList<>();
    private final Map<String, DateGroup> dateGroups = new LinkedHashMap<>();
    private OnTransactionClickListener clickListener;

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.clickListener = listener;
    }

    public void submitList(List<PendingTransaction> transactions) {
        items.clear();
        dateGroups.clear();

        if (transactions == null || transactions.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Group transactions by date
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (PendingTransaction txn : transactions) {
            String dateKey = dateKeyFormat.format(new Date(txn.getTransactionAt()));
            
            DateGroup group = dateGroups.get(dateKey);
            if (group == null) {
                group = new DateGroup(txn.getTransactionAt());
                dateGroups.put(dateKey, group);
            }
            group.addTransaction(txn);
        }

        // Build flat list with headers
        for (DateGroup group : dateGroups.values()) {
            items.add(group); // Header
            items.addAll(group.transactions); // Transactions
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof DateGroup ? TYPE_DATE_HEADER : TYPE_TRANSACTION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_transaction_new, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind((DateGroup) items.get(position));
        } else if (holder instanceof TransactionViewHolder) {
            PendingTransaction txn = (PendingTransaction) items.get(position);
            ((TransactionViewHolder) holder).bind(txn);
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTransactionClick(txn);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * Get item at position (can be DateGroup or PendingTransaction)
     */
    public Object getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }
    
    /**
     * Check if item at position is a transaction (not a date header)
     */
    public boolean isTransaction(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position) instanceof PendingTransaction;
        }
        return false;
    }
    
    /**
     * Get transaction at position (returns null if it's a date header)
     */
    public PendingTransaction getTransactionAt(int position) {
        Object item = getItemAt(position);
        if (item instanceof PendingTransaction) {
            return (PendingTransaction) item;
        }
        return null;
    }

    private String getCategoryColor(String category) {
        if (category == null) return "#607D8B";
        String color = CATEGORY_COLORS.get(category.toLowerCase().trim());
        return color != null ? color : "#607D8B";
    }

    // ==================== Date Group ====================

    static class DateGroup {
        final long timestamp;
        final List<PendingTransaction> transactions = new ArrayList<>();
        double totalIncome = 0;
        double totalExpense = 0;

        DateGroup(long timestamp) {
            this.timestamp = timestamp;
        }

        void addTransaction(PendingTransaction txn) {
            transactions.add(txn);
            if ("income".equals(txn.getType())) {
                totalIncome += txn.getAmount();
            } else {
                totalExpense += txn.getAmount();
            }
        }
    }

    // ==================== Date Header ViewHolder ====================

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDateNum;
        private final TextView tvDayName;
        private final TextView tvFullDate;
        private final TextView tvDayIncome;
        private final TextView tvDayExpense;

        private final SimpleDateFormat dayNumFormat = new SimpleDateFormat("dd", Locale.getDefault());
        private final SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM.yyyy", Locale.getDefault());

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateNum = itemView.findViewById(R.id.tvDateNum);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvFullDate = itemView.findViewById(R.id.tvFullDate);
            tvDayIncome = itemView.findViewById(R.id.tvDayIncome);
            tvDayExpense = itemView.findViewById(R.id.tvDayExpense);
        }

        void bind(DateGroup group) {
            Date date = new Date(group.timestamp);
            
            tvDateNum.setText(dayNumFormat.format(date));
            tvDayName.setText(dayNameFormat.format(date));
            tvFullDate.setText(monthYearFormat.format(date));
            
            tvDayIncome.setText(String.format(Locale.getDefault(), "₹ %.2f", group.totalIncome));
            tvDayExpense.setText(String.format(Locale.getDefault(), "₹ %.2f", group.totalExpense));
            
            // Hide income if zero
            tvDayIncome.setVisibility(group.totalIncome > 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }

    // ==================== Transaction ViewHolder ====================

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategoryTag;
        private final TextView tvMerchant;
        private final TextView tvTimeDate;
        private final TextView tvNote;
        private final TextView tvAmount;
        
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryTag = itemView.findViewById(R.id.tvCategoryTag);
            tvMerchant = itemView.findViewById(R.id.tvMerchant);
            tvTimeDate = itemView.findViewById(R.id.tvTimeDate);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }

        void bind(PendingTransaction txn) {
            // Category tag
            String category = txn.getCategory();
            if (category == null || category.isEmpty()) {
                category = "Other";
            }
            tvCategoryTag.setText(category);
            
            // Set category background color
            String colorHex = getCategoryColor(category);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(8f);
            bg.setColor(Color.parseColor(colorHex));
            tvCategoryTag.setBackground(bg);

            // Merchant name
            String merchant = txn.getMerchant();
            if (merchant == null || merchant.isEmpty()) {
                merchant = txn.getBankName() != null ? txn.getBankName() : "Transaction";
            }
            tvMerchant.setText(merchant);

            // Time
            tvTimeDate.setText(timeFormat.format(new Date(txn.getTransactionAt())));

            // Note (show if available)
            String note = txn.getNote();
            if (note != null && !note.isEmpty()) {
                tvNote.setText(note);
                tvNote.setVisibility(View.VISIBLE);
            } else {
                tvNote.setVisibility(View.GONE);
            }

            // Single amount - color based on type
            boolean isIncome = "income".equals(txn.getType());
            tvAmount.setText(String.format(Locale.getDefault(), "₹ %.2f", txn.getAmount()));
            tvAmount.setTextColor(Color.parseColor(isIncome ? "#4CAF50" : "#F44336"));
        }
    }
}
