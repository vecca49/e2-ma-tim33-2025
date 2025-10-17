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

    public interface OnTaskLoadListener {
        void onSuccess(Task task);
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

    // 🔸 Dohvatanje taska po ID-u
    public void getTaskById(String taskId, OnTaskLoadListener listener) {
        taskRepository.getTaskById(taskId, new TaskRepository.OnTaskLoadListener() {
            @Override
            public void onSuccess(Task task) {
                listener.onSuccess(task);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e.getMessage());
            }
        });
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
        if (task.getExecutionTime() == null || task.getStatus() != Task.TaskStatus.ACTIVE) return;

        Timestamp now = Timestamp.now();
        long diffMillis = now.toDate().getTime() - task.getExecutionTime().toDate().getTime();
        long daysDiff = diffMillis / (1000 * 60 * 60 * 24);

        if (daysDiff > 3) {
            task.setStatus(Task.TaskStatus.NOT_DONE);
            taskRepository.saveTask(task, new TaskRepository.OnTaskSaveListener() {
                @Override public void onSuccess() {
                    Log.d(TAG, "Task older than 3 days -> marked as NOT_DONE");
                }
                @Override public void onError(Exception e) {
                    Log.e(TAG, "Failed to auto-update task status", e);
                }
            });
        }
    }


    public void updateTask(Task task, OnTaskOperationListener listener) {
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


    public void editTask(Task task, String newName, String newDescription,
                         Timestamp newExecutionTime, Task.Difficulty newDifficulty,
                         Task.Importance newImportance,
                         OnTaskOperationListener listener) {

        if (task.getStatus() == Task.TaskStatus.DONE ||
                task.getStatus() == Task.TaskStatus.NOT_DONE ||
                task.getStatus() == Task.TaskStatus.CANCELED) {
            listener.onError("Ne možete menjati završene ili otkazane zadatke.");
            return;
        }

        Timestamp now = Timestamp.now();
        if (task.isRepeating() && task.getExecutionTime() != null &&
                task.getExecutionTime().compareTo(now) < 0) {
            listener.onError("Ne možete menjati prethodna ponavljanja zadatka.");
            return;
        }

        if (newName != null) task.setName(newName);
        if (newDescription != null) task.setDescription(newDescription);
        if (newExecutionTime != null) task.setExecutionTime(newExecutionTime);
        if (newDifficulty != null) task.setDifficulty(newDifficulty);
        if (newImportance != null) task.setImportance(newImportance);

        int totalXp = task.getDifficulty().getXp() + task.getImportance().getXp();
        task.setTotalXP(totalXp);

        updateTask(task, listener);
    }


    public void deleteTaskSafe(Task task, OnTaskOperationListener listener) {
        if (task.getStatus() == Task.TaskStatus.DONE ||
                task.getStatus() == Task.TaskStatus.NOT_DONE) {
            listener.onError("Ne možete brisati završene zadatke.");
            return;
        }

        if (task.isRepeating()) {
            Timestamp now = Timestamp.now();
            if (task.getExecutionTime() != null && task.getExecutionTime().compareTo(now) < 0) {
                listener.onError("Ne možete brisati prethodna ponavljanja zadatka.");
                return;
            }
        }

        deleteTask(task.getId(), listener);
    }

}
