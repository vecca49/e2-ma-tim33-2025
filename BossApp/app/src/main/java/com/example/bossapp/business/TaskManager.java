package com.example.bossapp.business;

import android.util.Log;
import com.example.bossapp.data.model.Task;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.model.XpQuotaTracker;
import com.example.bossapp.data.repository.TaskRepository;
import com.google.firebase.Timestamp;

import java.util.Calendar;
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

                if (newStatus == Task.TaskStatus.DONE) {
                    awardXPForCompletedTask(task);
                }

                listener.onSuccess();
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    private void awardXPForCompletedTask(Task task) {
        if (task.getStatus() == Task.TaskStatus.PAUSED ||
                task.getStatus() == Task.TaskStatus.CANCELED) {
            Log.d("XP", "XP se ne obračunava za pauzirane ili otkazane zadatke");
            return;
        }

        if (task.isXpAwarded()) {
            Log.d("XP", "XP je već dodijeljen za ovaj zadatak");
            return;
        }

        String userId = task.getOwnerId();
        UserManager userManager = new UserManager();
        TaskRepository taskRepository = new TaskRepository();

        taskRepository.getTasksByUser(userId, new TaskRepository.OnTasksLoadListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                Calendar nowCal = Calendar.getInstance();
                int currentDay = nowCal.get(Calendar.DAY_OF_YEAR);
                int currentWeek = nowCal.get(Calendar.WEEK_OF_YEAR);
                int currentMonth = nowCal.get(Calendar.MONTH);
                int currentYear = nowCal.get(Calendar.YEAR);

                // Brojači po tipovima i vremenskim periodima
                int veryEasyNormalCount = 0, easyImportantCount = 0;
                int hardVeryImportantCount = 0, extremeCount = 0, specialCount = 0;

                for (Task t : tasks) {
                    if (t.getExecutionTime() == null) continue;
                    if (t.getStatus() != Task.TaskStatus.DONE) continue;

                    Calendar tCal = Calendar.getInstance();
                    tCal.setTime(t.getExecutionTime().toDate());

                    // Dnevne kvote
                    if (tCal.get(Calendar.YEAR) == currentYear &&
                            tCal.get(Calendar.DAY_OF_YEAR) == currentDay) {

                        if (t.getDifficulty() == Task.Difficulty.VERY_EASY &&
                                t.getImportance() == Task.Importance.NORMAL) veryEasyNormalCount++;
                        if (t.getDifficulty() == Task.Difficulty.EASY &&
                                t.getImportance() == Task.Importance.IMPORTANT) easyImportantCount++;
                        if (t.getDifficulty() == Task.Difficulty.HARD &&
                                t.getImportance() == Task.Importance.VERY_IMPORTANT) hardVeryImportantCount++;
                    }

                    // Nedeljne kvote
                    if (tCal.get(Calendar.YEAR) == currentYear &&
                            tCal.get(Calendar.WEEK_OF_YEAR) == currentWeek) {
                        if (t.getDifficulty() == Task.Difficulty.EXTREME) extremeCount++;
                    }

                    // Mesečne kvote
                    if (tCal.get(Calendar.YEAR) == currentYear &&
                            tCal.get(Calendar.MONTH) == currentMonth) {
                        if (t.getImportance() == Task.Importance.SPECIAL) specialCount++;
                    }
                }

                // Vrednost XP-a koju ćemo dodati
                final int[] earnedXp = {0};

                // Računanje XP za trenutni zadatak
                if (task.getDifficulty() == Task.Difficulty.VERY_EASY &&
                        task.getImportance() == Task.Importance.NORMAL &&
                        veryEasyNormalCount < 5) {
                    earnedXp[0] = task.getTotalXP();
                } else if (task.getDifficulty() == Task.Difficulty.EASY &&
                        task.getImportance() == Task.Importance.IMPORTANT &&
                        easyImportantCount < 5) {
                    earnedXp[0] = task.getTotalXP();
                } else if (task.getDifficulty() == Task.Difficulty.HARD &&
                        task.getImportance() == Task.Importance.VERY_IMPORTANT &&
                        hardVeryImportantCount < 2) {
                    earnedXp[0] = task.getTotalXP();
                } else if (task.getDifficulty() == Task.Difficulty.EXTREME &&
                        extremeCount < 1) {
                    earnedXp[0] = task.getTotalXP();
                } else if (task.getImportance() == Task.Importance.SPECIAL &&
                        specialCount < 1) {
                    earnedXp[0] = task.getTotalXP();
                } else {
                    Log.d("XP", "Korisnik je prešao kvotu za ovaj tip zadatka, XP se ne dodaje.");
                    return;
                }

                // Dodavanje XP korisniku
                userManager.getUserById(userId, new UserManager.OnUserLoadListener() {
                    @Override
                    public void onSuccess(User user) {
                        if (user == null) return;

                        user.setXp(user.getXp() + earnedXp[0]);
                        userManager.updateUser(user, new UserManager.OnUserOperationListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("XP", "Korisniku dodano " + earnedXp[0] + " XP");

                                task.setXpAwarded(true);
                                taskRepository.saveTask(task, new TaskRepository.OnTaskSaveListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d("XP", "Task označen kao XP nagrađen");
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.e("XP", "Greška pri označavanju taska: " + e.getMessage());
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                Log.e("XP", "Greška pri ažuriranju korisnika: " + message);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("XP", "Greška pri dohvatanju korisnika: " + message);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("XP", "Greška pri čitanju taskova: " + e.getMessage());
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
