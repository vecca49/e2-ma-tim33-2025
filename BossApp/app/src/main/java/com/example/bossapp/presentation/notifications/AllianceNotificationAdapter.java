package com.example.bossapp.presentation.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.AllianceNotification;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllianceNotificationAdapter extends RecyclerView.Adapter<AllianceNotificationAdapter.ViewHolder> {

    private List<AllianceNotification> notifications;
    private OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onDismiss(AllianceNotification notification);
    }

    public AllianceNotificationAdapter(List<AllianceNotification> notifications,
                                       OnNotificationActionListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllianceNotification notification = notifications.get(position);

        // Set icon based on type
        if ("alliance_accepted".equals(notification.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_alliance);
        } else if ("alliance_declined".equals(notification.getType())) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_delete);
        }

        holder.tvMessage.setText(notification.getMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(notification.getTimestamp()));
        holder.tvTimestamp.setText(dateStr);

        holder.btnDismiss.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDismiss(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvMessage;
        TextView tvTimestamp;
        MaterialButton btnDismiss;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnDismiss = itemView.findViewById(R.id.btnDismiss);
        }
    }
}