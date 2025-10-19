package com.example.bossapp.business;

import android.util.Log;
import com.example.bossapp.data.model.Task;
import com.example.bossapp.data.model.User;
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
        Log.d(TAG, "=== UPDATE TASK STATUS START ===");
        Log.d(TAG, "Task: " + task.getName());
        Log.d(TAG, "Current status: " + task.getStatus());
        Log.d(TAG, "New status: " + newStatus);

        if (task.getStatus() == Task.TaskStatus.DONE ||
                task.getStatus() == Task.TaskStatus.CANCELED ||
                task.getStatus() == Task.TaskStatus.NOT_DONE) {
            Log.e(TAG, "Task već završen/otkazan - ne može se promeniti!");
            listener.onError("Completed or canceled tasks cannot be changed");
            return;
        }

        task.setStatus(newStatus);
        taskRepository.saveTask(task, new TaskRepository.OnTaskSaveListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Task status saved successfully");

                if (newStatus == Task.TaskStatus.DONE) {
                    Log.d(TAG, "Task je DONE - pozivam awardXPForCompletedTask");
                    awardXPForCompletedTask(task);
                } else {
                    Log.d(TAG, "Task nije DONE - XP se ne dodaje");
                }

                listener.onSuccess();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Greška pri čuvanju task statusa: " + e.getMessage());
                listener.onError(e.getMessage());
            }
        });
    }

    private void awardXPForCompletedTask(Task task) {
        Log.d(TAG, "=== AWARD XP START ===");
        Log.d(TAG, "Task: " + task.getName());
        Log.d(TAG, "Task status: " + task.getStatus());
        Log.d(TAG, "Task xpAwarded: " + task.isXpAwarded());

        if (task.getStatus() == Task.TaskStatus.PAUSED ||
                task.getStatus() == Task.TaskStatus.CANCELED) {
            Log.d(TAG, "XP se ne obračunava za pauzirane ili otkazane zadatke");
            return;
        }

        if (task.isXpAwarded()) {
            Log.d(TAG, "XP je već dodijeljen za ovaj zadatak");
            return;
        }

        String userId = task.getOwnerId();
        Log.d(TAG, "User ID: " + userId);

        UserManager userManager = new UserManager();
        LevelManager levelManager = new LevelManager();
        TaskRepository taskRepository = new TaskRepository();

        // Prvo učitaj korisnika
        Log.d(TAG, "Učitavanje korisnika...");
        userManager.getUserById(userId, new UserManager.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "Korisnik učitan: " + user.getUsername());
                Log.d(TAG, "Trenutni XP: " + user.getXp());
                Log.d(TAG, "Trenutni Level: " + user.getLevel());

                // Učitaj sve taskove za proveru kvote
                Log.d(TAG, "Učitavanje taskova za proveru kvote...");
                taskRepository.getTasksByUser(userId, new TaskRepository.OnTasksLoadListener() {
                    @Override
                    public void onSuccess(List<Task> tasks) {
                        Log.d(TAG, "Učitano taskova: " + tasks.size());

                        Calendar nowCal = Calendar.getInstance();
                        int currentDay = nowCal.get(Calendar.DAY_OF_YEAR);
                        int currentWeek = nowCal.get(Calendar.WEEK_OF_YEAR);
                        int currentMonth = nowCal.get(Calendar.MONTH);
                        int currentYear = nowCal.get(Calendar.YEAR);

                        int veryEasyNormalCount = 0, easyImportantCount = 0;
                        int hardVeryImportantCount = 0, extremeCount = 0, specialCount = 0;

                        for (Task t : tasks) {
                            if (t.getExecutionTime() == null) continue;
                            if (t.getStatus() != Task.TaskStatus.DONE) continue;
                            if (!t.isXpAwarded()) continue;

                            Calendar tCal = Calendar.getInstance();
                            tCal.setTime(t.getExecutionTime().toDate());

                            if (tCal.get(Calendar.YEAR) == currentYear &&
                                    tCal.get(Calendar.DAY_OF_YEAR) == currentDay) {

                                if (t.getDifficulty() == Task.Difficulty.VERY_EASY &&
                                        t.getImportance() == Task.Importance.NORMAL) {
                                    veryEasyNormalCount++;
                                }
                                if (t.getDifficulty() == Task.Difficulty.EASY &&
                                        t.getImportance() == Task.Importance.IMPORTANT) {
                                    easyImportantCount++;
                                }
                                if (t.getDifficulty() == Task.Difficulty.HARD &&
                                        t.getImportance() == Task.Importance.VERY_IMPORTANT) {
                                    hardVeryImportantCount++;
                                }
                            }

                            if (tCal.get(Calendar.YEAR) == currentYear &&
                                    tCal.get(Calendar.WEEK_OF_YEAR) == currentWeek) {
                                if (t.getDifficulty() == Task.Difficulty.EXTREME) {
                                    extremeCount++;
                                }
                            }

                            if (tCal.get(Calendar.YEAR) == currentYear &&
                                    tCal.get(Calendar.MONTH) == currentMonth) {
                                if (t.getImportance() == Task.Importance.SPECIAL) {
                                    specialCount++;
                                }
                            }
                        }

                        Log.d(TAG, "=== KVOTE ===");
                        Log.d(TAG, "Very Easy + Normal: " + veryEasyNormalCount + "/5");
                        Log.d(TAG, "Easy + Important: " + easyImportantCount + "/5");
                        Log.d(TAG, "Hard + Very Important: " + hardVeryImportantCount + "/2");
                        Log.d(TAG, "Extreme: " + extremeCount + "/1 (weekly)");
                        Log.d(TAG, "Special: " + specialCount + "/1 (monthly)");

                        boolean shouldAwardXP = false;
                        String quotaReason = "";

                        if (task.getDifficulty() == Task.Difficulty.VERY_EASY &&
                                task.getImportance() == Task.Importance.NORMAL) {
                            if (veryEasyNormalCount < 5) {
                                shouldAwardXP = true;
                                quotaReason = "Very Easy + Normal: " + (veryEasyNormalCount + 1) + "/5";
                            } else {
                                quotaReason = "Very Easy + Normal kvota prešla: " + veryEasyNormalCount + "/5";
                            }
                        } else if (task.getDifficulty() == Task.Difficulty.EASY &&
                                task.getImportance() == Task.Importance.IMPORTANT) {
                            if (easyImportantCount < 5) {
                                shouldAwardXP = true;
                                quotaReason = "Easy + Important: " + (easyImportantCount + 1) + "/5";
                            } else {
                                quotaReason = "Easy + Important kvota prešla: " + easyImportantCount + "/5";
                            }
                        } else if (task.getDifficulty() == Task.Difficulty.HARD &&
                                task.getImportance() == Task.Importance.VERY_IMPORTANT) {
                            if (hardVeryImportantCount < 2) {
                                shouldAwardXP = true;
                                quotaReason = "Hard + Very Important: " + (hardVeryImportantCount + 1) + "/2";
                            } else {
                                quotaReason = "Hard + Very Important kvota prešla: " + hardVeryImportantCount + "/2";
                            }
                        } else if (task.getDifficulty() == Task.Difficulty.EXTREME) {
                            if (extremeCount < 1) {
                                shouldAwardXP = true;
                                quotaReason = "Extreme: " + (extremeCount + 1) + "/1 (weekly)";
                            } else {
                                quotaReason = "Extreme kvota prešla: " + extremeCount + "/1 (weekly)";
                            }
                        } else if (task.getImportance() == Task.Importance.SPECIAL) {
                            if (specialCount < 1) {
                                shouldAwardXP = true;
                                quotaReason = "Special: " + (specialCount + 1) + "/1 (monthly)";
                            } else {
                                quotaReason = "Special kvota prešla: " + specialCount + "/1 (monthly)";
                            }
                        } else {
                            shouldAwardXP = true;
                            quotaReason = "Task ne spada u specifičnu kvotu - XP se dodaje";
                        }

                        Log.d(TAG, "Kvota razlog: " + quotaReason);
                        Log.d(TAG, "Dodeli XP: " + shouldAwardXP);

                        if (!shouldAwardXP) {
                            Log.d(TAG, "Kvota prelazena - XP se ne dodaje");
                            return;
                        }

                        int currentLevel = user.getLevel();
                        int difficultyXP = LevelManager.calculateDifficultyXP(task.getDifficulty(), currentLevel);
                        int importanceXP = LevelManager.calculateImportanceXP(task.getImportance(), currentLevel);
                        int totalXP = difficultyXP + importanceXP;

                        Log.d(TAG, "=== XP CALCULATION ===");
                        Log.d(TAG, "Current Level: " + currentLevel);
                        Log.d(TAG, "Difficulty: " + task.getDifficulty() + " = " + difficultyXP + " XP");
                        Log.d(TAG, "Importance: " + task.getImportance() + " = " + importanceXP + " XP");
                        Log.d(TAG, "Total XP: " + totalXP);
                        Log.d(TAG, "Old User XP: " + user.getXp());
                        Log.d(TAG, "New User XP: " + (user.getXp() + totalXP));

                        user.setXp(user.getXp() + totalXP);

                        Log.d(TAG, "=== AFTER XP ADDED ===");
                        Log.d(TAG, "User XP now: " + user.getXp());
                        Log.d(TAG, "User Level: " + user.getLevel());
                        Log.d(TAG, "Required for next level: " + LevelManager.calculateXPForLevel(user.getLevel() + 1));

                        Log.d(TAG, "Čuvanje korisnika...");
                        userManager.updateUser(user, new UserManager.OnUserOperationListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "✅ Korisnik uspešno ažuriran sa novim XP!");

                                // *** PRVO: Proveri level up ***
                                Log.d(TAG, "Proveravam level up...");
                                levelManager.checkAndProcessLevelUp(user, new LevelManager.OnLevelUpListener() {
                                    @Override
                                    public void onLevelUp(int newLevel, String newTitle, int ppGained, int coinsGained) {
                                        Log.d(TAG, "🎉🎉🎉 LEVEL UP! 🎉🎉🎉");
                                        Log.d(TAG, "Novi nivo: " + newLevel);
                                        Log.d(TAG, "Nova titula: " + newTitle);
                                        Log.d(TAG, "Dobijeni PP: " + ppGained);

                                        // NAKON level up-a, označi task
                                        markTaskAsXpAwarded(task, taskRepository);
                                    }

                                    @Override
                                    public void onNoLevelUp() {
                                        Log.d(TAG, "Još nije vreme za level up");
                                        // I dalje označi task kao awarded
                                        markTaskAsXpAwarded(task, taskRepository);
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Log.e(TAG, "❌ Greška pri level up proveri: " + message);
                                        // Ipak pokušaj da označiš task
                                        markTaskAsXpAwarded(task, taskRepository);
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                Log.e(TAG, "❌ Greška pri ažuriranju korisnika: " + message);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "❌ Greška pri učitavanju taskova: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "❌ Greška pri učitavanju korisnika: " + message);
            }
        });
    }

    private void markTaskAsXpAwarded(Task task, TaskRepository taskRepository) {
        task.setXpAwarded(true);
        taskRepository.saveTask(task, new TaskRepository.OnTaskSaveListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Task označen kao XP nagrađen");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Greška pri označavanju taska: " + e.getMessage());
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
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Task older than 3 days -> marked as NOT_DONE");
                }

                @Override
                public void onError(Exception e) {
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

    public void calculateSuccessRate(String userId, OnSuccessRateCalculatedListener listener) {
        getUserTasks(userId, new OnTasksLoadListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                int completed = 0;
                int total = 0;

                for (Task t : tasks) {
                    if (t.getStatus() == Task.TaskStatus.PAUSED || t.getStatus() == Task.TaskStatus.CANCELED)
                        continue;

                    if (t.isWithinQuota()) {
                        total++;
                        if (t.getStatus() == Task.TaskStatus.DONE) {
                            completed++;
                        }
                    }
                }

                int successRate = total > 0 ? (completed * 100) / total : 0;
                listener.onCalculated(successRate);
            }

            @Override
            public void onError(String message) {
                listener.onCalculated(0);
            }
        });
    }


    public interface OnSuccessRateCalculatedListener {
        void onCalculated(int successRate);
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
