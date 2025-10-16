package com.example.bossapp.presentation.task;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.data.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TaskCalendarActivity extends AppCompatActivity {

    private TaskManager taskManager;
    private String userId;
    private List<Task> tasks = new ArrayList<>();
    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private MaterialCalendarView calendarView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_calendar);

        taskManager = new TaskManager();
        userId = FirebaseAuth.getInstance().getUid();

        rvTasks = findViewById(R.id.rvTasksForDate);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new ArrayList<>());
        rvTasks.setAdapter(adapter);

        calendarView = findViewById(R.id.calendarView);

        taskManager.getUserTasks(userId, new TaskManager.OnTasksLoadListener() {
            @Override
            public void onSuccess(List<Task> loadedTasks) {
                tasks.clear();
                tasks.addAll(loadedTasks);

                markTasksInCalendar(loadedTasks);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(TaskCalendarActivity.this, "Greška pri učitavanju zadataka: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            List<Task> selectedTasks = new ArrayList<>();
            for (Task task : tasks) {
                if (task.getExecutionTime() != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(task.getExecutionTime().toDate());
                    if (cal.get(Calendar.YEAR) == date.getYear() &&
                            cal.get(Calendar.MONTH) == date.getMonth() &&
                            cal.get(Calendar.DAY_OF_MONTH) == date.getDay()) {
                        selectedTasks.add(task);
                    }
                }
            }

            adapter = new TaskAdapter(selectedTasks);
            rvTasks.setAdapter(adapter);
        });
    }

    private void markTasksInCalendar(List<Task> loadedTasks) {
        Map<String, HashSet<CalendarDay>> colorDates = new HashMap<>();

        for (Task task : loadedTasks) {
            if (task.getExecutionTime() != null && task.getCategoryColor() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(task.getExecutionTime().toDate());

                CalendarDay day = CalendarDay.from(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                );

                colorDates.putIfAbsent(task.getCategoryColor(), new HashSet<>());
                colorDates.get(task.getCategoryColor()).add(day);
            }
        }

        for (Map.Entry<String, HashSet<CalendarDay>> entry : colorDates.entrySet()) {
            try {
                int color = android.graphics.Color.parseColor(entry.getKey());
                calendarView.addDecorator(new TaskDayDecorator(entry.getValue(), color));
            } catch (IllegalArgumentException e) {
                calendarView.addDecorator(new TaskDayDecorator(entry.getValue(), android.graphics.Color.GRAY));
            }
        }
    }
}
