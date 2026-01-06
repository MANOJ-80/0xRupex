package com.rupex.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.rupex.app.R;
import com.rupex.app.data.local.entity.PendingTransaction;
import com.rupex.app.databinding.ItemTransactionBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for transactions
 */
public class TransactionAdapter extends ListAdapter<PendingTransaction, TransactionAdapter.ViewHolder> {

    public interface OnTransactionClickListener {
        void onTransactionClick(PendingTransaction transaction);
    }

    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    // Category emoji icons
    private static final Map<String, String> CATEGORY_ICONS = new HashMap<String, String>() {{
        put("food", "🍕");
        put("grocery", "🛒");
        put("groceries", "🛒");
        put("shopping", "🛍️");
        put("transport", "🚗");
        put("transportation", "🚗");
        put("bills", "📄");
        put("utilities", "💡");
        put("entertainment", "🎬");
        put("health", "💊");
        put("education", "📚");
        put("travel", "✈️");
        put("salary", "💰");
        put("income", "💵");
        put("investment", "📈");
        put("rent", "🏠");
        put("subscription", "📱");
        put("friends", "👥");
        put("gift", "🎁");
        put("other", "📦");
        put("uncategorized", "❓");
    }};

    private OnTransactionClickListener clickListener;

    public TransactionAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.clickListener = listener;
    }

    private static final DiffUtil.ItemCallback<PendingTransaction> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<PendingTransaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull PendingTransaction oldItem, 
                                       @NonNull PendingTransaction newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PendingTransaction oldItem, 
                                          @NonNull PendingTransaction newItem) {
            return oldItem.getAmount() == newItem.getAmount()
                    && oldItem.getType().equals(newItem.getType())
                    && oldItem.isSynced() == newItem.isSynced()
                    && (oldItem.getCategory() == null ? newItem.getCategory() == null 
                        : oldItem.getCategory().equals(newItem.getCategory()))
                    && (oldItem.getNote() == null ? newItem.getNote() == null
                        : oldItem.getNote().equals(newItem.getNote()));
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingTransaction transaction = getItem(position);
        holder.bind(transaction);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTransactionClick(transaction);
            }
        });
    }

    private static String getCategoryIcon(String category) {
        if (category == null || category.isEmpty()) return "❓";
        String icon = CATEGORY_ICONS.get(category.toLowerCase().trim());
        return icon != null ? icon : "📦";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        ViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PendingTransaction transaction) {
            // Amount with sign and color
            String amountText;
            int amountColor;
            
            if ("income".equals(transaction.getType())) {
                amountText = String.format(Locale.getDefault(), "+₹%.0f", transaction.getAmount());
                amountColor = binding.getRoot().getContext().getColor(R.color.income);
            } else {
                amountText = String.format(Locale.getDefault(), "-₹%.0f", transaction.getAmount());
                amountColor = binding.getRoot().getContext().getColor(R.color.expense);
            }
            
            binding.textAmount.setText(amountText);
            binding.textAmount.setTextColor(amountColor);

            // Merchant or bank name
            String title = transaction.getMerchant();
            if (title == null || title.isEmpty()) {
                title = transaction.getBankName();
            }
            if (title == null || title.isEmpty()) {
                title = transaction.getType().equals("income") ? "Income" : "Expense";
            }
            binding.textMerchant.setText(title);

            // Date and time
            String dateText = DATE_FORMAT.format(new Date(transaction.getTransactionAt()));
            binding.textDate.setText(dateText);

            // Category icon
            String category = transaction.getCategory();
            binding.tvCategoryIcon.setText(getCategoryIcon(category));
            
            // Category chip
            if (category != null && !category.isEmpty()) {
                binding.tvCategory.setText(category);
                binding.tvCategory.setVisibility(View.VISIBLE);
            } else {
                binding.tvCategory.setVisibility(View.GONE);
            }

            // Note (if exists)
            String note = transaction.getNote();
            if (note != null && !note.isEmpty()) {
                binding.tvNote.setText("📝 " + note);
                binding.tvNote.setVisibility(View.VISIBLE);
            } else {
                binding.tvNote.setVisibility(View.GONE);
            }

            // Sync status indicator - REMOVED in UI refactor
            // if (transaction.isSynced()) {
            //     binding.iconSyncStatus.setImageResource(R.drawable.ic_check);
            //     binding.iconSyncStatus.setColorFilter(
            //             binding.getRoot().getContext().getColor(R.color.success)
            //     );
            // } else {
            //     binding.iconSyncStatus.setImageResource(R.drawable.ic_sync);
            //     binding.iconSyncStatus.setColorFilter(
            //             binding.getRoot().getContext().getColor(R.color.warning)
            //     );
            // }

            // Account info (hidden by default now)
            // if (transaction.getLast4Digits() != null) {
            //     binding.textAccount.setText("••" + transaction.getLast4Digits());
            // }
        }
    }
}
