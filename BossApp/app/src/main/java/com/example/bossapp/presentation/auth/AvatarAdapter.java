package com.example.bossapp.presentation.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private int[] avatarResources;
    private int selectedPosition = 0;
    private OnAvatarSelectedListener listener;

    public interface OnAvatarSelectedListener {
        void onAvatarSelected(int position);
    }

    public AvatarAdapter(int[] avatarResources, OnAvatarSelectedListener listener) {
        this.avatarResources = avatarResources;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        holder.ivAvatar.setImageResource(avatarResources[position]);

        // Highlight izabrani avatar
        if (position == selectedPosition) {
            holder.cardView.setCardBackgroundColor(
                    holder.itemView.getContext().getColor(R.color.primary));
            holder.cardView.setCardElevation(8f);
        } else {
            holder.cardView.setCardBackgroundColor(
                    holder.itemView.getContext().getColor(R.color.card_background));
            holder.cardView.setCardElevation(2f);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            listener.onAvatarSelected(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return avatarResources.length;
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivAvatar;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardAvatar);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }
}
