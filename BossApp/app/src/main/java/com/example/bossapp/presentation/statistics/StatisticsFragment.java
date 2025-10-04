package com.example.bossapp.presentation.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bossapp.R;
import com.example.bossapp.presentation.base.BaseFragment;

public class StatisticsFragment extends BaseFragment {

    private TextView tvActiveDays, tvTotalTasks, tvLongestStreak;
    private TextView tvCompletedTasks, tvPendingTasks, tvCancelledTasks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);
        loadStatistics();
    }

    private void initViews(View view) {
        tvActiveDays = view.findViewById(R.id.tvActiveDays);
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks);
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak);
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = view.findViewById(R.id.tvPendingTasks);
        tvCancelledTasks = view.findViewById(R.id.tvCancelledTasks);
    }

    private void loadStatistics() {
        tvActiveDays.setText("0");
        tvTotalTasks.setText("0");
        tvLongestStreak.setText("0");
        tvCompletedTasks.setText("0");
        tvPendingTasks.setText("0");
        tvCancelledTasks.setText("0");
    }
}