package com.example.bossapp.presentation.task;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.data.model.Category;
import com.example.bossapp.data.model.EventDecorator;
import com.example.bossapp.data.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
                            (cal.get(Calendar.MONTH) + 1) == date.getMonth() &&
                            cal.get(Calendar.DAY_OF_MONTH) == date.getDay()) {
                        selectedTasks.add(task);
                    }
                }
            }
            adapter.updateTasks(selectedTasks);
        });

    }



    public void updateTaskColorsForCategory(Category category) {
        for (Task task : tasks) {
            if (task.getCategoryId().equals(category.getId())) {
                task.setCategoryColor(category.getColorHex());
            }
        }
        markTasksInCalendar(tasks);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }


    private void markTasksInCalendar(List<Task> loadedTasks) {
        calendarView.removeDecorators();

        Map<CalendarDay, Integer> dayColorMap = new HashMap<>();

        for (Task task : loadedTasks) {
            if (task.getExecutionTime() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(task.getExecutionTime().toDate());

                CalendarDay day = CalendarDay.from(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                );

                int color;
                try {
                    color = Color.parseColor(task.getCategoryColor());
                } catch (IllegalArgumentException e) {
                    color = Color.GRAY;
                }


                dayColorMap.putIfAbsent(day, color);
            }
        }

        for (Map.Entry<CalendarDay, Integer> entry : dayColorMap.entrySet()) {
            calendarView.addDecorator(new EventDecorator(entry.getValue(), entry.getKey()));
        }
    }

}



