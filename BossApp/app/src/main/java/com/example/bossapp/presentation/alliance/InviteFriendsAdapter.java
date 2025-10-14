package com.example.bossapp.presentation.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.Alliance;
import com.example.bossapp.data.model.User;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class InviteFriendsAdapter extends RecyclerView.Adapter<InviteFriendsAdapter.ViewHolder> {

    private List<User> friends;
    private Alliance currentAlliance;
    private OnInviteListener listener;

    public interface OnInviteListener {
        void onInvite(User user);
        void onRemove(User user);
    }

    public InviteFriendsAdapter(List<User> friends, Alliance currentAlliance, OnInviteListener listener) {
        this.friends = friends;
        this.currentAlliance = currentAlliance;
        this.listener = listener;
    }

    public void updateAlliance(Alliance alliance) {
        this.currentAlliance = alliance;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invite_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = friends.get(position);

        int[] avatarResources = {
                R.drawable.avatar,
                R.drawable.bow,
                R.drawable.magician,
                R.drawable.pinkily,
                R.drawable.swordsman
        };
        holder.ivAvatar.setImageResource(avatarResources[user.getAvatarIndex()]);

        holder.tvUsername.setText(user.getUsername());
        holder.tvLevel.setText("Level " + user.getLevel());

        // Check if user is in current alliance
        boolean isInAlliance = currentAlliance != null &&
                currentAlliance.isMember(user.getUserId());

        if (isInAlliance) {
            holder.btnInvite.setText("Remove");
            holder.btnInvite.setIcon(holder.itemView.getContext().getDrawable(android.R.drawable.ic_delete));
            holder.btnInvite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemove(user);
                }
            });
        } else {
            holder.btnInvite.setText("Invite");
            holder.btnInvite.setIcon(holder.itemView.getContext().getDrawable(android.R.drawable.ic_input_add));
            holder.btnInvite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onInvite(user);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername;
        TextView tvLevel;
        MaterialButton btnInvite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            btnInvite = itemView.findViewById(R.id.btnInvite);
        }
    }
}