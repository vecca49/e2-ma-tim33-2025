package com.example.bossapp.presentation.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.AllianceInvitation;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllianceInvitationAdapter extends RecyclerView.Adapter<AllianceInvitationAdapter.ViewHolder> {

    private List<AllianceInvitation> invitations;
    private OnInvitationActionListener listener;

    public interface OnInvitationActionListener {
        void onAccept(AllianceInvitation invitation);
        void onDecline(AllianceInvitation invitation);
    }

    public AllianceInvitationAdapter(List<AllianceInvitation> invitations, OnInvitationActionListener listener) {
        this.invitations = invitations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_invitation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllianceInvitation invitation = invitations.get(position);

        holder.tvAllianceName.setText(invitation.getAllianceName());
        holder.tvSenderName.setText(invitation.getSenderUsername());
        holder.tvMessage.setText("invited you to join their alliance");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateStr = sdf.format(new Date(invitation.getTimestamp()));
        holder.tvTimestamp.setText(dateStr);

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(invitation);
            }
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecline(invitation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAllianceName;
        TextView tvSenderName;
        TextView tvMessage;
        TextView tvTimestamp;
        MaterialButton btnAccept;
        MaterialButton btnDecline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAllianceName = itemView.findViewById(R.id.tvAllianceName);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}