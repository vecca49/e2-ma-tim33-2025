package com.example.bossapp.presentation.task;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.data.model.Task;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    public void updateTasks(List<Task> newTasks) {
        this.tasks.clear();
        this.tasks.addAll(newTasks);
        notifyDataSetChanged();
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
                tvDate.setText("Ponavljajući zadatak");

            itemView.setOnClickListener(v -> {
                if (task.getId() != null) {
                    Intent intent = new Intent(v.getContext(), TaskInfoActivity.class);
                    intent.putExtra("TASK_ID", task.getId());
                    v.getContext().startActivity(intent);
                } else {
                    Toast.makeText(v.getContext(), "Zadatak nema ID!", Toast.LENGTH_SHORT).show();
                }
            });

            itemView.setOnLongClickListener(v -> {
                showTaskDetails(v.getContext(), task);
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
                    .setNegativeButton("Promijeni status", (dialog, which) -> handleTaskStatusChange(context, task))
                    .setNeutralButton("Uredi", (dialog, which) -> {
                        Intent intent = new Intent(context.getApplicationContext(), TaskDetailActivity.class);
                        intent.putExtra("taskId", task.getId());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.getApplicationContext().startActivity(intent);
                    })
                    .show();
        }

        private void handleTaskStatusChange(Context context, Task task) {
            if (task.getStatus() == Task.TaskStatus.DONE ||
                    task.getStatus() == Task.TaskStatus.CANCELED ||
                    task.getStatus() == Task.TaskStatus.NOT_DONE) {
                Toast.makeText(context, "Ovaj zadatak se više ne može menjati.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (task.getExecutionTime() != null && isOlderThanThreeDays(task.getExecutionTime())) {
                taskManager.updateTaskStatus(task, Task.TaskStatus.NOT_DONE, new TaskManager.OnTaskOperationListener() {
                    @Override
                    public void onSuccess() {
                        task.setStatus(Task.TaskStatus.NOT_DONE);
                        notifyItemChanged(getAdapterPosition());
                        Toast.makeText(context, "Zadatak automatski označen kao neurađen.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(context, "Greška: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            String[] statuses;
            if (task.isRepeating()) {
                statuses = new String[]{"Aktivan", "Pauziran", "Urađen", "Otkazan"};
            } else {
                statuses = new String[]{"Aktivan", "Urađen", "Otkazan"};
            }

            new AlertDialog.Builder(context)
                    .setTitle("Promijeni status zadatka")
                    .setItems(statuses, (dialog, which) -> {
                        Task.TaskStatus newStatus = null;

                        switch (statuses[which]) {
                            case "Aktivan":
                                newStatus = Task.TaskStatus.ACTIVE;
                                break;
                            case "Pauziran":
                                newStatus = Task.TaskStatus.PAUSED;
                                break;
                            case "Urađen":
                                newStatus = Task.TaskStatus.DONE;
                                break;
                            case "Otkazan":
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
                                    Toast.makeText(context, "Status ažuriran: " + finalStatus.name(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(context, "Greška: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Otkaži", null)
                    .show();
        }

        private boolean isOlderThanThreeDays(Timestamp timestamp) {
            Calendar limit = Calendar.getInstance();
            limit.add(Calendar.DAY_OF_YEAR, -3);
            return timestamp.toDate().before(limit.getTime());
        }
    }
}
