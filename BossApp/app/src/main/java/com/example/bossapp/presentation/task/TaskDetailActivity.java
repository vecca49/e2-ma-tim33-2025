package com.example.bossapp.presentation.task;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bossapp.R;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.data.model.Task;
import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskDetailActivity extends AppCompatActivity {

    private TaskManager taskManager;
    private Task currentTask;

    private EditText etName, etDescription, etExecutionTime;
    private Spinner spDifficulty, spImportance;
    private Button btnSave, btnDelete;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        taskManager = new TaskManager();

        etName = findViewById(R.id.etTaskName);
        etDescription = findViewById(R.id.etTaskDescription);
        etExecutionTime = findViewById(R.id.etExecutionTime);
        spDifficulty = findViewById(R.id.spDifficulty);
        spImportance = findViewById(R.id.spImportance);
        btnSave = findViewById(R.id.btnSaveChanges);
        btnDelete = findViewById(R.id.btnDeleteTask);

        spDifficulty.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Task.Difficulty.values()));
        spImportance.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Task.Importance.values()));

        String taskId = getIntent().getStringExtra("taskId");
        if (taskId != null) {
            loadTask(taskId);
        } else {
            Toast.makeText(this, "Nema prosleđenog ID zadatka.", Toast.LENGTH_LONG).show();
        }

        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> deleteTask());
    }

    private void loadTask(String taskId) {
        taskManager.getTaskById(taskId, new TaskManager.OnTaskLoadListener() {
            @Override
            public void onSuccess(Task task) {
                currentTask = task;
                populateFields(task);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(TaskDetailActivity.this, "Greška: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void populateFields(Task task) {
        etName.setText(task.getName());
        etDescription.setText(task.getDescription());
        if (task.getExecutionTime() != null)
            etExecutionTime.setText(sdf.format(task.getExecutionTime().toDate()));

        if (task.getDifficulty() != null)
            spDifficulty.setSelection(task.getDifficulty().ordinal());

        if (task.getImportance() != null)
            spImportance.setSelection(task.getImportance().ordinal());

        if (task.getStatus() == Task.TaskStatus.DONE ||
                task.getStatus() == Task.TaskStatus.NOT_DONE ||
                (task.isRepeating() && task.getEndDate() != null &&
                        task.getEndDate().compareTo(Timestamp.now()) < 0)) {
            etName.setEnabled(false);
            etDescription.setEnabled(false);
            etExecutionTime.setEnabled(false);
            spDifficulty.setEnabled(false);
            spImportance.setEnabled(false);
            btnSave.setEnabled(false);
            btnDelete.setEnabled(false);
        }
    }

    private void saveChanges() {
        if (currentTask == null) return;

        currentTask.setName(etName.getText().toString().trim());
        currentTask.setDescription(etDescription.getText().toString().trim());
        currentTask.setDifficulty(Task.Difficulty.values()[spDifficulty.getSelectedItemPosition()]);
        currentTask.setImportance(Task.Importance.values()[spImportance.getSelectedItemPosition()]);

        String execTimeStr = etExecutionTime.getText().toString().trim();
        if (!execTimeStr.isEmpty()) {
            try {
                Date date = sdf.parse(execTimeStr);
                if (date != null) {
                    currentTask.setExecutionTime(new Timestamp(date));
                }
            } catch (ParseException e) {
                Toast.makeText(this, "Neispravan format vremena", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        taskManager.createOrUpdateTask(currentTask, new TaskManager.OnTaskOperationListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(TaskDetailActivity.this, "Zadatak uspešno ažuriran", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();            }

            @Override
            public void onError(String message) {
                Toast.makeText(TaskDetailActivity.this, "Greška: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteTask() {
        if (currentTask == null) return;

        taskManager.deleteTask(currentTask.getId(), new TaskManager.OnTaskOperationListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(TaskDetailActivity.this, "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();            }

            @Override
            public void onError(String message) {
                Toast.makeText(TaskDetailActivity.this, "Greška: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
