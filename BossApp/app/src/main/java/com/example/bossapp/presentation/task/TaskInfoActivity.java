package com.example.bossapp.presentation.task;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bossapp.R;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.data.model.Task;
import com.google.firebase.Timestamp;

public class TaskInfoActivity extends AppCompatActivity {

    private TaskManager taskManager;
    private Task task;

    private TextView tvName, tvDescription, tvStatus;
    private Button btnDone, btnCancel, btnPause, btnActivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_info);

        taskManager = new TaskManager();

        tvName = findViewById(R.id.tvTaskName);
        tvDescription = findViewById(R.id.tvTaskDescription);
        tvStatus = findViewById(R.id.tvTaskStatus);
        btnDone = findViewById(R.id.btnMarkDone);
        btnCancel = findViewById(R.id.btnCancelTask);
        btnPause = findViewById(R.id.btnPauseTask);
        btnActivate = findViewById(R.id.btnActivateTask);

        String taskId = getIntent().getStringExtra("TASK_ID");
        if (taskId == null) {
            Toast.makeText(this, "Zadatak nije pronađen!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskManager.getTaskById(taskId, new TaskManager.OnTaskLoadListener() {
            @Override
            public void onSuccess(Task loadedTask) {
                task = loadedTask;
                taskManager.checkIfTaskExpired(task);
                showTaskDetails();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(TaskInfoActivity.this, "Greška: " + message, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnDone.setOnClickListener(v -> updateStatus(Task.TaskStatus.DONE));
        btnCancel.setOnClickListener(v -> updateStatus(Task.TaskStatus.CANCELED));
        btnPause.setOnClickListener(v -> updateStatus(Task.TaskStatus.PAUSED));
        btnActivate.setOnClickListener(v -> updateStatus(Task.TaskStatus.ACTIVE));
    }

    private void showTaskDetails() {
        tvName.setText(task.getName());
        tvDescription.setText(task.getDescription());
        tvStatus.setText("Status: " + task.getStatus().name());

        btnDone.setEnabled(task.getStatus() == Task.TaskStatus.ACTIVE);
        btnCancel.setEnabled(task.getStatus() == Task.TaskStatus.ACTIVE);
        btnPause.setEnabled(task.isRepeating() && task.getStatus() == Task.TaskStatus.ACTIVE);
        btnActivate.setEnabled(task.getStatus() == Task.TaskStatus.PAUSED);
    }

    private void updateStatus(Task.TaskStatus newStatus) {
        if (!task.canChangeStatus()) {
            Toast.makeText(this, "Ovaj zadatak se više ne može menjati.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (task.getStatus() == Task.TaskStatus.PAUSED && newStatus != Task.TaskStatus.ACTIVE) {
            Toast.makeText(this, "Pauziran zadatak može samo biti aktiviran.", Toast.LENGTH_SHORT).show();
            return;
        }

        taskManager.updateTaskStatus(task, newStatus, new TaskManager.OnTaskOperationListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(TaskInfoActivity.this, "Status ažuriran!", Toast.LENGTH_SHORT).show();
                task.setStatus(newStatus);
                showTaskDetails();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(TaskInfoActivity.this, "Greška: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
