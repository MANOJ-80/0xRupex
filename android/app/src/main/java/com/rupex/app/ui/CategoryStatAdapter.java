package com.rupex.app.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.rupex.app.R;
import com.rupex.app.data.model.CategoryStat;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for displaying category spending statistics with donut chart style
 */
public class CategoryStatAdapter extends ListAdapter<CategoryStat, CategoryStatAdapter.ViewHolder> {

    // Chart colors - warm palette like the screenshot
    public static final int[] CHART_COLORS = new int[]{
            Color.parseColor("#FFB5A7"), // Peach/salmon (Food)
            Color.parseColor("#FFD93D"), // Yellow (Other)
            Color.parseColor("#F8B739"), // Orange (Education)
            Color.parseColor("#A8E6CF"), // Mint green (Social Life)
            Color.parseColor("#DDA0DD"), // Plum (Friends)
            Color.parseColor("#87CEEB"), // Sky blue (Transport)
            Color.parseColor("#98D8C8"), // Teal (Bills)
            Color.parseColor("#F7DC6F"), // Light yellow (Grocery)
            Color.parseColor("#BB8FCE"), // Light purple (Shopping)
            Color.parseColor("#85C1E9"), // Light blue (Entertainment)
    };

    // Category emoji icons
    private static final Map<String, String> CATEGORY_ICONS = new HashMap<String, String>() {{
        put("food", "🍕");
        put("other", "📦");
        put("education", "📚");
        put("social life", "🎉");
        put("friends", "👥");
        put("transport", "🚗");
        put("transportation", "🚗");
        put("bills", "📄");
        put("utilities", "💡");
        put("grocery", "🛒");
        put("groceries", "🛒");
        put("shopping", "🛍️");
        put("entertainment", "🎬");
        put("health", "💊");
        put("fitness", "💪");
        put("travel", "✈️");
        put("salary", "💰");
        put("income", "💵");
        put("investment", "📈");
        put("gift", "🎁");
        put("rent", "🏠");
        put("subscription", "📱");
        put("insurance", "🛡️");
        put("uncategorized", "❓");
    }};

    private final NumberFormat currencyFormat;

    public CategoryStatAdapter() {
        super(DIFF_CALLBACK);
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setMaximumFractionDigits(0);
    }

    private static final DiffUtil.ItemCallback<CategoryStat> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CategoryStat>() {
                @Override
                public boolean areItemsTheSame(@NonNull CategoryStat oldItem, @NonNull CategoryStat newItem) {
                    return oldItem.getCategoryId() == newItem.getCategoryId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CategoryStat oldItem, @NonNull CategoryStat newItem) {
                    return oldItem.getTotal() == newItem.getTotal() &&
                            oldItem.getPercentage() == newItem.getPercentage();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_stat_new, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryStat stat = getItem(position);
        holder.bind(stat, position);
    }

    private String getCategoryIcon(String categoryName) {
        if (categoryName == null) return "📦";
        String icon = CATEGORY_ICONS.get(categoryName.toLowerCase().trim());
        return icon != null ? icon : "📦";
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPercentBadge;
        private final TextView tvCategoryIcon;
        private final TextView tvCategoryName;
        private final TextView tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPercentBadge = itemView.findViewById(R.id.tvPercentBadge);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }

        void bind(CategoryStat stat, int position) {
            // Percentage badge
            int percentInt = (int) Math.round(stat.getPercentage());
            tvPercentBadge.setText(percentInt + "%");
            
            // Set badge background color
            int color = CHART_COLORS[position % CHART_COLORS.length];
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(12f);
            badgeBg.setColor(color);
            tvPercentBadge.setBackground(badgeBg);

            // Category icon
            tvCategoryIcon.setText(getCategoryIcon(stat.getCategoryName()));
            
            // Category name
            tvCategoryName.setText(stat.getCategoryName());
            
            // Amount
            tvAmount.setText(currencyFormat.format(stat.getTotal()));
        }
    }
}
