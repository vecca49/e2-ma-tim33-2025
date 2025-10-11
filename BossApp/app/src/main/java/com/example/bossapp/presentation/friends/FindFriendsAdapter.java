package com.example.bossapp.presentation.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.User;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class FindFriendsAdapter extends RecyclerView.Adapter<FindFriendsAdapter.ViewHolder> {

    private List<User> users;
    private User currentUser;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onAddFriend(User user);
        void onRemoveFriend(User user);
        void onViewProfile(User user);
    }

    public FindFriendsAdapter(List<User> users, User currentUser, OnUserActionListener listener) {
        this.users = users;
        this.currentUser = currentUser;
        this.listener = listener;
    }

    public void updateCurrentUser(User user) {
        this.currentUser = user;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_find_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);

        int[] avatarResources = {
                R.drawable.avatar,
                R.drawable.bow,
                R.drawable.magician,
                R.drawable.pinkily,
                R.drawable.swordsman
        };
        holder.ivAvatar.setImageResource(avatarResources[user.getAvatarIndex()]);

        holder.tvUsername.setText(user.getUsername());
        holder.tvLevel.setText("Lvl " + user.getLevel());

        // Check if already friends
        boolean isFriend = currentUser != null && currentUser.isFriend(user.getUserId());

        if (isFriend) {
            // Already friends - show Remove button
            holder.btnFriendAction.setText("Remove");
            holder.btnFriendAction.setIcon(holder.itemView.getContext().getDrawable(android.R.drawable.ic_delete));
            holder.btnFriendAction.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            holder.btnFriendAction.setContentDescription("Remove " + user.getUsername() + " from friends");
            holder.btnFriendAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveFriend(user);
                }
            });
        } else {
            // Not friends - show Add button
            holder.btnFriendAction.setText("Add");
            holder.btnFriendAction.setIcon(holder.itemView.getContext().getDrawable(android.R.drawable.ic_input_add));
            holder.btnFriendAction.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            holder.btnFriendAction.setContentDescription("Add " + user.getUsername() + " as friend");
            holder.btnFriendAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddFriend(user);
                }
            });
        }

        // Click on card to view profile
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProfile(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername;
        TextView tvLevel;
        MaterialButton btnFriendAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            btnFriendAction = itemView.findViewById(R.id.btnFriendAction);
        }
    }
}