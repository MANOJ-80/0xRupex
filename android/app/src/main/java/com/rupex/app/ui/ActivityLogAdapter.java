package com.rupex.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.rupex.app.R;
import com.rupex.app.data.local.entity.ActivityLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Adapter for displaying activity logs
 */
public class ActivityLogAdapter extends ListAdapter<ActivityLog, ActivityLogAdapter.LogViewHolder> {

    private static final DiffUtil.ItemCallback<ActivityLog> DIFF_CALLBACK = new DiffUtil.ItemCallback<ActivityLog>() {
        @Override
        public boolean areItemsTheSame(@NonNull ActivityLog oldItem, @NonNull ActivityLog newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ActivityLog oldItem, @NonNull ActivityLog newItem) {
            return oldItem.getType().equals(newItem.getType()) &&
                   oldItem.getMessage().equals(newItem.getMessage()) &&
                   oldItem.getTimestamp() == newItem.getTimestamp();
        }
    };

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public ActivityLogAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        ActivityLog log = getItem(position);
        holder.bind(log);
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvType;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final TextView tvDetails;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvLogType);
            tvMessage = itemView.findViewById(R.id.tvLogMessage);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            tvDetails = itemView.findViewById(R.id.tvLogDetails);
        }

        void bind(ActivityLog log) {
            // Set type with color
            String typeText = log.getType().toUpperCase();
            tvType.setText(typeText);
            
            int colorRes;
            switch (log.getType()) {
                case "captured":
                    colorRes = R.color.primary;
                    break;
                case "added":
                    colorRes = R.color.income;
                    break;
                case "rejected":
                    colorRes = R.color.expense;
                    break;
                default:
                    colorRes = R.color.text_secondary;
            }
            tvType.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));

            // Set message
            tvMessage.setText(log.getMessage());

            // Set time
            String timeStr = timeFormat.format(new Date(log.getTimestamp()));
            tvTime.setText(timeStr);

            // Set details
            StringBuilder details = new StringBuilder();
            if (log.getSource() != null) {
                details.append("Source: ").append(log.getSource());
            }
            if (log.getAmount() != null && log.getAmount() > 0) {
                if (details.length() > 0) details.append(" • ");
                details.append("₹").append(String.format(Locale.getDefault(), "%.2f", log.getAmount()));
            }
            if (log.getMerchant() != null && !log.getMerchant().isEmpty()) {
                if (details.length() > 0) details.append(" • ");
                details.append(log.getMerchant());
            }
            if (log.getReason() != null && !log.getReason().isEmpty()) {
                if (details.length() > 0) details.append("\n");
                details.append("Reason: ").append(log.getReason());
            }
            
            if (details.length() > 0) {
                tvDetails.setText(details.toString());
                tvDetails.setVisibility(View.VISIBLE);
            } else {
                tvDetails.setVisibility(View.GONE);
            }
        }
    }
}

