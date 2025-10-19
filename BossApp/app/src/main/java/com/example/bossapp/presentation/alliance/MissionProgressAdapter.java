package com.example.bossapp.presentation.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.MemberProgress;

import java.util.List;

public class MissionProgressAdapter extends RecyclerView.Adapter<MissionProgressAdapter.ViewHolder> {

    private final List<MemberProgress> members;

    public MissionProgressAdapter(List<MemberProgress> members) {
        this.members = members;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemberProgress mp = members.get(position);
        holder.tvUsername.setText(mp.getUsername());
        holder.tvDamage.setText("Damage: " + mp.calculateTotalDamage());
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvDamage;
        ViewHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvDamage = itemView.findViewById(R.id.tvDamage);
        }
    }
}
