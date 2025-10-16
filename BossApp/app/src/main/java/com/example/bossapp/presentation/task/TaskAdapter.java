package com.example.bossapp.presentation.task;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.data.model.Task;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private final TaskManager taskManager = new TaskManager();

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvDate;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvDate = itemView.findViewById(R.id.tvTaskDate);
        }

        public void bind(Task task) {
            tvName.setText(task.getName());
            tvStatus.setText(task.getStatus().name());
            if (task.getExecutionTime() != null)
                tvDate.setText(sdf.format(task.getExecutionTime().toDate()));
            else
                tvDate.setText("Repeating task");

            itemView.setOnClickListener(v -> showTaskDetails(v.getContext(), task));

            itemView.setOnLongClickListener(v -> {
                showStatusDialog(v.getContext(), task);
                return true;
            });
        }

        private void showTaskDetails(Context context, Task task) {
            String details = "Opis: " + task.getDescription() + "\n"
                    + "Kategorija: " + task.getCategoryName() + "\n"
                    + "Težina: " + task.getDifficulty() + "\n"
                    + "Važnost: " + task.getImportance() + "\n"
                    + "Status: " + task.getStatus();

            new AlertDialog.Builder(context)
                    .setTitle(task.getName())
                    .setMessage(details)
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Promeni status", (dialog, which) -> showStatusDialog(context, task))
                    .setNeutralButton("Change", (dialog, which) -> {
                        Intent intent = new Intent(context.getApplicationContext(), TaskDetailActivity.class);
                        intent.putExtra("taskId", task.getId());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 🔹 neophodno
                        context.getApplicationContext().startActivity(intent);
                    })
                    .show();
        }


        private void showStatusDialog(Context context, Task task) {
            String[] statuses = {"Aktivno", "Pauzirano", "Urađeno", "Otkazano"};
            new AlertDialog.Builder(context)
                    .setTitle("Promijeni status zadatka")
                    .setItems(statuses, (dialog, which) -> {
                        Task.TaskStatus newStatus = null;
                        switch (which) {
                            case 0:
                                newStatus = Task.TaskStatus.ACTIVE;
                                break;
                            case 1:
                                newStatus = Task.TaskStatus.PAUSED;
                                break;
                            case 2:
                                newStatus = Task.TaskStatus.DONE;
                                break;
                            case 3:
                                newStatus = Task.TaskStatus.CANCELED;
                                break;
                        }

                        if (newStatus != null) {
                            Task.TaskStatus finalStatus = newStatus;
                            taskManager.updateTaskStatus(task, newStatus, new TaskManager.OnTaskOperationListener() {
                                @Override
                                public void onSuccess() {
                                    task.setStatus(finalStatus);
                                    notifyItemChanged(getAdapterPosition());
                                    Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Otkaži", null)
                    .show();
        }
    }
}
