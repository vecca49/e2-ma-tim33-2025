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
        void onOpenChat(AllianceNotification notification);
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
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvMessage;
        TextView tvTimestamp;
        MaterialButton btnAction;
        MaterialButton btnDismiss;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTimestamp = itemView.findViewById(R.id.tvNotificationTimestamp);
            btnAction = itemView.findViewById(R.id.btnNotificationAction);
            btnDismiss = itemView.findViewById(R.id.btnDismiss);
        }

        void bind(AllianceNotification notification, OnNotificationActionListener listener) {
            tvMessage.setText(notification.getMessage());
            tvTimestamp.setText(formatTime(notification.getTimestamp()));

            // Set icon based on notification type
            switch (notification.getType()) {
                case "alliance_accepted":
                    ivIcon.setImageResource(R.drawable.ic_alliance);
                    btnAction.setVisibility(View.GONE);
                    break;
                case "alliance_declined":
                    ivIcon.setImageResource(android.R.drawable.ic_delete);
                    btnAction.setVisibility(View.GONE);
                    break;
                case "alliance_message":
                    ivIcon.setImageResource(android.R.drawable.ic_menu_send);
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText("Open Chat");
                    btnAction.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onOpenChat(notification);
                        }
                    });
                    break;
                default:
                    ivIcon.setImageResource(android.R.drawable.ic_dialog_info);
                    btnAction.setVisibility(View.GONE);
                    break;
            }

            btnDismiss.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDismiss(notification);
                }
            });
        }

        private String formatTime(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}