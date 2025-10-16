package com.example.bossapp.presentation.task;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bossapp.R;
import com.example.bossapp.business.CategoryManager;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.data.model.Category;
import com.example.bossapp.data.model.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskFragment extends Fragment {

    private FloatingActionButton fabAdd;
    private TaskManager taskManager;
    private CategoryManager categoryManager;
    private String userId;

    private List<Task> tasks = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    public TaskFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task, container, false);

        taskManager = new TaskManager();
        categoryManager = new CategoryManager();
        userId = FirebaseAuth.getInstance().getUid();

        loadCategories();
        refreshTasks();
        TabLayout tabLayout = v.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = v.findViewById(R.id.viewPager);
        fabAdd = v.findViewById(R.id.fabAddTask);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new OneTimeTasksFragment());
        fragments.add(new RepeatingTasksFragment());

        TaskTabsAdapter adapter = new TaskTabsAdapter(this, fragments);
        viewPager.setAdapter(adapter);

        tabLayout.addTab(tabLayout.newTab().setText("One-time"));
        tabLayout.addTab(tabLayout.newTab().setText("Repeating"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });

        fabAdd.setOnClickListener(view -> showAddTaskDialog());
        return v;
    }





    private void loadCategories() {
        categoryManager.loadUserCategories(userId, new CategoryManager.OnCategoriesLoadListener() {
            @Override
            public void onSuccess(List<Category> loadedCategories) {
                categories.clear();
                if (loadedCategories.isEmpty()) {
                    categories.add(new Category("General", "#2196F3", userId));
                } else {
                    categories.addAll(loadedCategories);
                }
            }


            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create Task");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etTaskName);
        EditText etDesc = dialogView.findViewById(R.id.etTaskDesc);
        Spinner spnDifficulty = dialogView.findViewById(R.id.spnDifficulty);
        Spinner spnImportance = dialogView.findViewById(R.id.spnImportance);
        Spinner spnCategory = dialogView.findViewById(R.id.spnCategory);
        CheckBox chkRepeating = dialogView.findViewById(R.id.chkRepeating);
        LinearLayout layoutRepeating = dialogView.findViewById(R.id.layoutRepeatingOptions);
        EditText etRepeatInterval = dialogView.findViewById(R.id.etRepeatInterval);
        Spinner spnRepeatUnit = dialogView.findViewById(R.id.spnRepeatUnit);
        Button btnPickTime = dialogView.findViewById(R.id.btnPickTime);
        Button btnStartDate = dialogView.findViewById(R.id.btnStartDate);
        Button btnEndDate = dialogView.findViewById(R.id.btnEndDate);

        Timestamp[] selectedTime = new Timestamp[1];
        Timestamp[] startDate = new Timestamp[1];
        Timestamp[] endDate = new Timestamp[1];

        chkRepeating.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutRepeating.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            btnPickTime.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        btnPickTime.setOnClickListener(v -> pickDateTime(selectedTime));
        btnStartDate.setOnClickListener(v -> pickDate(startDate));
        btnEndDate.setOnClickListener(v -> pickDate(endDate));

        ArrayAdapter<Task.Difficulty> diffAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, Task.Difficulty.values());
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDifficulty.setAdapter(diffAdapter);

        ArrayAdapter<Task.Importance> impAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, Task.Importance.values());
        impAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnImportance.setAdapter(impAdapter);

        ArrayAdapter<Category> catAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategory.setAdapter(catAdapter);

        ArrayAdapter<Task.RepeatUnit> repeatAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, Task.RepeatUnit.values());
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnRepeatUnit.setAdapter(repeatAdapter);

        builder.setPositiveButton("Save", (dialog, which) -> {
            if (etName.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Enter task name", Toast.LENGTH_SHORT).show();
                return;
            }

            Task task = new Task();
            task.setName(etName.getText().toString());
            task.setDescription(etDesc.getText().toString());

            Category selectedCategory = (Category) spnCategory.getSelectedItem();
            task.setCategoryId(selectedCategory.getId());
            task.setCategoryName(selectedCategory.getName());
            task.setCategoryColor(selectedCategory.getColorHex());

            task.setDifficulty((Task.Difficulty) spnDifficulty.getSelectedItem());
            task.setImportance((Task.Importance) spnImportance.getSelectedItem());
            task.setOwnerId(userId);
            task.setStatus(Task.TaskStatus.ACTIVE);

            boolean isRepeating = chkRepeating.isChecked();
            task.setRepeating(isRepeating);

            if (isRepeating) {
                if (etRepeatInterval.getText().toString().isEmpty() ||
                        startDate[0] == null || endDate[0] == null) {
                    Toast.makeText(getContext(), "Enter all repeat fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                task.setRepeatInterval(Integer.parseInt(etRepeatInterval.getText().toString()));
                task.setRepeatUnit((Task.RepeatUnit) spnRepeatUnit.getSelectedItem());
                task.setStartDate(startDate[0]);
                task.setEndDate(endDate[0]);
            } else {
                if (selectedTime[0] == null) {
                    Toast.makeText(getContext(), "Select execution time", Toast.LENGTH_SHORT).show();
                    return;
                }
                task.setExecutionTime(selectedTime[0]);
            }

            taskManager.createOrUpdateTask(task, new TaskManager.OnTaskOperationListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Task created successfully", Toast.LENGTH_SHORT).show();
                    refreshTasks();                }

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void pickDateTime(Timestamp[] result) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    TimePickerDialog timePicker = new TimePickerDialog(getContext(),
                            (timeView, hour, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                calendar.set(Calendar.MINUTE, minute);
                                result[0] = new Timestamp(calendar.getTime());
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true);
                    timePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    public void refreshTasks() {
        List<Fragment> fragments = getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof OneTimeTasksFragment) {
                ((OneTimeTasksFragment) fragment).loadTasks();
            } else if (fragment instanceof RepeatingTasksFragment) {
                ((RepeatingTasksFragment) fragment).loadTasks();
            }
        }
    }


    private void pickDate(Timestamp[] result) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    result[0] = new Timestamp(calendar.getTime());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private ActivityResultLauncher<Intent> taskDetailLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        taskManager = new TaskManager();
        categoryManager = new CategoryManager();
        userId = FirebaseAuth.getInstance().getUid();

        // Launcher za TaskDetailActivity
        taskDetailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        refreshTasks(); // 🔹 Osvježava liste nakon promjene
                    }
                });
    }

    public void openTaskDetail(String taskId) {
        Intent intent = new Intent(getContext(), TaskDetailActivity.class);
        intent.putExtra("taskId", taskId);
        taskDetailLauncher.launch(intent);
    }


}
