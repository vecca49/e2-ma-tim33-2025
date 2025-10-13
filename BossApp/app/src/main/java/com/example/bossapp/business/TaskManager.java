package com.example.bossapp.business;

import android.util.Log;
import com.example.bossapp.data.model.Task;
import com.example.bossapp.data.repository.TaskRepository;
import com.google.firebase.Timestamp;

import java.util.List;

public class TaskManager {
    private static final String TAG = "TaskManager";
    private final TaskRepository taskRepository;

    public TaskManager() {
        this.taskRepository = new TaskRepository();
    }

    public interface OnTaskOperationListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnTasksLoadListener {
        void onSuccess(List<Task> tasks);
        void onError(String message);
    }

    public void createOrUpdateTask(Task task, OnTaskOperationListener listener) {
        try {
            int totalXp = task.getDifficulty().getXp() + task.getImportance().getXp();
            task.setTotalXP(totalXp);

            if (task.isRepeating()) {
                if (task.getRepeatUnit() == null || task.getRepeatInterval() <= 0) {
                    listener.onError("Invalid repeat settings");
                    return;
                }
                if (task.getStartDate() == null || task.getEndDate() == null) {
                    listener.onError("Missing start or end date for repeating task");
                    return;
                }
            } else {
                if (task.getExecutionTime() == null) {
                    listener.onError("Execution time required for one-time task");
                    return;
                }
            }

            taskRepository.saveTask(task, new TaskRepository.OnTaskSaveListener() {
                @Override
                public void onSuccess() {
                    listener.onSuccess();
                }

                @Override
                public void onError(Exception e) {
                    listener.onError(e.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onError("Error saving task: " + e.getMessage());
        }
    }

    public void getUserTasks(String userId, OnTasksLoadListener listener) {
        taskRepository.getTasksByUser(userId, new TaskRepository.OnTasksLoadListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                listener.onSuccess(tasks);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    public void deleteTask(String taskId, OnTaskOperationListener listener) {
        taskRepository.deleteTask(taskId, new TaskRepository.OnTaskDeleteListener() {
            @Override
            public void onSuccess() {
                listener.onSuccess();
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    public void updateTaskStatus(Task task, Task.TaskStatus newStatus, OnTaskOperationListener listener) {
        if (task.getStatus() == Task.TaskStatus.DONE ||
                task.getStatus() == Task.TaskStatus.CANCELED ||
                task.getStatus() == Task.TaskStatus.NOT_DONE) {
            listener.onError("Completed or canceled tasks cannot be changed");
            return;
        }

        task.setStatus(newStatus);
        taskRepository.saveTask(task, new TaskRepository.OnTaskSaveListener() {
            @Override
            public void onSuccess() {
                listener.onSuccess();
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    public void checkIfTaskExpired(Task task) {
        Timestamp now = Timestamp.now();
        if (task.getExecutionTime() != null && task.getExecutionTime().compareTo(now) < 0 &&
                task.getStatus() == Task.TaskStatus.ACTIVE) {
            task.setStatus(Task.TaskStatus.NOT_DONE);
            taskRepository.saveTask(task, new TaskRepository.OnTaskSaveListener() {
                @Override public void onSuccess() {
                    Log.d(TAG, "Task marked as NOT_DONE automatically");
                }
                @Override public void onError(Exception e) {
                    Log.e(TAG, "Failed to update task status", e);
                }
            });
        }
    }
}

