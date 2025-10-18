package com.example.bossapp.presentation.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bossapp.R;
import com.example.bossapp.business.StatisticsManager;
import com.example.bossapp.presentation.base.BaseFragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsFragment extends BaseFragment {

    private TextView tvActiveDays, tvCurrentStreak, tvTotalTasks, tvLongestStreak;
    private TextView tvCompletedTasks, tvPendingTasks, tvCancelledTasks, tvNotDoneTasks;
    private TextView tvAverageDifficulty;

    private PieChart pieChartTaskStatus;
    private BarChart barChartCategories;
    private LineChart lineChartXP;

    private StatisticsManager statisticsManager;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        statisticsManager = new StatisticsManager();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadStatistics();
    }

    private void initViews(View view) {
        tvActiveDays = view.findViewById(R.id.tvActiveDays);
        tvCurrentStreak = view.findViewById(R.id.tvCurrentStreak);
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks);
        tvLongestStreak = view.findViewById(R.id.tvLongestStreak);
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = view.findViewById(R.id.tvPendingTasks);
        tvCancelledTasks = view.findViewById(R.id.tvCancelledTasks);
        tvNotDoneTasks = view.findViewById(R.id.tvNotDoneTasks);
        tvAverageDifficulty = view.findViewById(R.id.tvAverageDifficulty);

        pieChartTaskStatus = view.findViewById(R.id.pieChartTaskStatus);
        barChartCategories = view.findViewById(R.id.barChartCategories);
        lineChartXP = view.findViewById(R.id.lineChartXP);
    }

    private void loadStatistics() {
        statisticsManager.loadStatistics(currentUserId,
                new StatisticsManager.OnStatisticsLoadListener() {
                    @Override
                    public void onSuccess(StatisticsManager.Statistics statistics) {
                        displayStatistics(statistics);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(),
                                "Error loading statistics: " + message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayStatistics(StatisticsManager.Statistics stats) {
        // Brojke
        tvActiveDays.setText(String.valueOf(stats.activeDays));
        tvCurrentStreak.setText(String.valueOf(stats.currentStreak));
        tvTotalTasks.setText(String.valueOf(stats.totalTasks));
        tvLongestStreak.setText(String.valueOf(stats.longestStreak));
        tvCompletedTasks.setText(String.valueOf(stats.completedTasks));
        tvPendingTasks.setText(String.valueOf(stats.pendingTasks));
        tvCancelledTasks.setText(String.valueOf(stats.canceledTasks));
        tvNotDoneTasks.setText(String.valueOf(stats.notDoneTasks));
        tvAverageDifficulty.setText(String.format("%.1f XP", stats.averageDifficulty));

        // Grafikoni
        setupPieChart(stats);
        setupBarChart(stats);
        setupLineChart(stats);
    }

    private void setupPieChart(StatisticsManager.Statistics stats) {
        List<PieEntry> entries = new ArrayList<>();

        if (stats.completedTasks > 0)
            entries.add(new PieEntry(stats.completedTasks, "Completed"));
        if (stats.pendingTasks > 0)
            entries.add(new PieEntry(stats.pendingTasks, "Pending"));
        if (stats.canceledTasks > 0)
            entries.add(new PieEntry(stats.canceledTasks, "Cancelled"));
        if (stats.notDoneTasks > 0)
            entries.add(new PieEntry(stats.notDoneTasks, "Not Done"));

        if (entries.isEmpty()) {
            pieChartTaskStatus.setVisibility(View.GONE);
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#4CAF50"),  // Green - Completed
                Color.parseColor("#FF9800"),  // Orange - Pending
                Color.parseColor("#F44336"),  // Red - Cancelled
                Color.parseColor("#9E9E9E")   // Gray - Not Done
        );
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChartTaskStatus.setData(data);
        pieChartTaskStatus.setDrawHoleEnabled(true);
        pieChartTaskStatus.setHoleRadius(40f);
        pieChartTaskStatus.setTransparentCircleRadius(45f);

        Description desc = new Description();
        desc.setText("");
        pieChartTaskStatus.setDescription(desc);

        pieChartTaskStatus.animateY(1000);
        pieChartTaskStatus.invalidate();
    }

    private void setupBarChart(StatisticsManager.Statistics stats) {
        if (stats.tasksByCategory.isEmpty()) {
            barChartCategories.setVisibility(View.GONE);
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Integer> entry : stats.tasksByCategory.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Tasks by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        barChartCategories.setData(data);

        XAxis xAxis = barChartCategories.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);

        Description desc = new Description();
        desc.setText("");
        barChartCategories.setDescription(desc);

        barChartCategories.animateY(1000);
        barChartCategories.invalidate();
    }

    private void setupLineChart(StatisticsManager.Statistics stats) {
        if (stats.last7DaysXP.isEmpty()) {
            lineChartXP.setVisibility(View.GONE);
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < stats.last7DaysXP.size(); i++) {
            StatisticsManager.DailyXP dailyXP = stats.last7DaysXP.get(i);
            entries.add(new Entry(i, dailyXP.xp));
            labels.add(dailyXP.date);
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP Last 7 Days");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#2196F3"));
        dataSet.setFillAlpha(50);

        LineData data = new LineData(dataSet);
        lineChartXP.setData(data);

        XAxis xAxis = lineChartXP.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        Description desc = new Description();
        desc.setText("");
        lineChartXP.setDescription(desc);

        lineChartXP.animateX(1000);
        lineChartXP.invalidate();
    }
}