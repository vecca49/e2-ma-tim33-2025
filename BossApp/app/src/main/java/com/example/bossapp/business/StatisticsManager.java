package com.example.bossapp.business;

import android.util.Log;
import com.example.bossapp.data.model.Task;
import com.example.bossapp.data.repository.TaskRepository;

import java.util.*;

public class StatisticsManager {
    private static final String TAG = "StatisticsManager";
    private final TaskRepository taskRepository;

    public StatisticsManager() {
        this.taskRepository = new TaskRepository();
    }

    public interface OnStatisticsLoadListener {
        void onSuccess(Statistics statistics);
        void onError(String message);
    }

    public static class Statistics {
        public int activeDays;
        public int totalTasks;
        public int completedTasks;
        public int pendingTasks;
        public int canceledTasks;
        public int notDoneTasks;
        public int longestStreak;
        public int currentStreak;
        public Map<String, Integer> tasksByCategory;
        public List<DailyXP> last7DaysXP;
        public double averageDifficulty;

        public Statistics() {
            tasksByCategory = new HashMap<>();
            last7DaysXP = new ArrayList<>();
        }
    }

    public static class DailyXP {
        public String date;
        public int xp;

        public DailyXP(String date, int xp) {
            this.date = date;
            this.xp = xp;
        }
    }

    public void loadStatistics(String userId, OnStatisticsLoadListener listener) {
        taskRepository.getTasksByUser(userId, new TaskRepository.OnTasksLoadListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                Statistics stats = calculateStatistics(tasks);
                listener.onSuccess(stats);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    private Statistics calculateStatistics(List<Task> tasks) {
        Statistics stats = new Statistics();

        if (tasks.isEmpty()) {
            return stats;
        }

        // 1. Broj ukupno kreiranih, urađenih, neurađenih i otkazanih zadataka
        stats.totalTasks = tasks.size();
        for (Task task : tasks) {
            if (task.getStatus() == Task.TaskStatus.DONE) {
                stats.completedTasks++;
            } else if (task.getStatus() == Task.TaskStatus.ACTIVE || task.getStatus() == Task.TaskStatus.PAUSED) {
                stats.pendingTasks++;
            } else if (task.getStatus() == Task.TaskStatus.CANCELED) {
                stats.canceledTasks++;
            } else if (task.getStatus() == Task.TaskStatus.NOT_DONE) {
                stats.notDoneTasks++;
            }
        }

        // 2. Aktivni dani i streak
        calculateStreaks(tasks, stats);

        // 3. Broj završenih zadataka po kategoriji
        for (Task task : tasks) {
            if (task.getStatus() == Task.TaskStatus.DONE && task.getCategoryName() != null) {
                String category = task.getCategoryName();
                stats.tasksByCategory.put(category,
                        stats.tasksByCategory.getOrDefault(category, 0) + 1);
            }
        }

        // 4. Prosečna težina završenih zadataka
        calculateAverageDifficulty(tasks, stats);

        // 5. XP u poslednjih 7 dana
        calculateLast7DaysXP(tasks, stats);

        return stats;
    }

    private void calculateStreaks(List<Task> tasks, Statistics stats) {
        // Grupiši taskove po danima
        Map<String, List<Task>> tasksByDate = new TreeMap<>();

        for (Task task : tasks) {
            if (task.getExecutionTime() == null) continue;

            Calendar cal = Calendar.getInstance();
            cal.setTime(task.getExecutionTime().toDate());
            String dateKey = String.format("%04d-%02d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));

            tasksByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(task);
        }

        // Izračunaj najduži niz i trenutni niz
        int currentStreak = 0;
        int longestStreak = 0;
        String lastDate = null;

        List<String> sortedDates = new ArrayList<>(tasksByDate.keySet());
        Collections.sort(sortedDates);

        for (String date : sortedDates) {
            List<Task> dayTasks = tasksByDate.get(date);

            // Proveri da li postoji bar jedan završen zadatak tog dana
            boolean hasCompletedTask = false;
            for (Task task : dayTasks) {
                if (task.getStatus() == Task.TaskStatus.DONE) {
                    hasCompletedTask = true;
                    break;
                }
            }

            if (hasCompletedTask) {
                if (lastDate == null || isConsecutiveDay(lastDate, date)) {
                    currentStreak++;
                    longestStreak = Math.max(longestStreak, currentStreak);
                } else {
                    currentStreak = 1;
                }
                lastDate = date;
            }
            // Niz se ne prekida ako nema zadataka tog dana
        }

        stats.activeDays = tasksByDate.size();
        stats.currentStreak = currentStreak;
        stats.longestStreak = longestStreak;
    }

    private boolean isConsecutiveDay(String date1, String date2) {
        try {
            String[] parts1 = date1.split("-");
            String[] parts2 = date2.split("-");

            Calendar cal1 = Calendar.getInstance();
            cal1.set(Integer.parseInt(parts1[0]),
                    Integer.parseInt(parts1[1]) - 1,
                    Integer.parseInt(parts1[2]));

            Calendar cal2 = Calendar.getInstance();
            cal2.set(Integer.parseInt(parts2[0]),
                    Integer.parseInt(parts2[1]) - 1,
                    Integer.parseInt(parts2[2]));

            long diff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
            long daysDiff = diff / (1000 * 60 * 60 * 24);

            return daysDiff == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private void calculateAverageDifficulty(List<Task> tasks, Statistics stats) {
        int totalDifficultyXP = 0;
        int completedCount = 0;

        for (Task task : tasks) {
            if (task.getStatus() == Task.TaskStatus.DONE) {
                totalDifficultyXP += task.getDifficulty().getXp();
                completedCount++;
            }
        }

        stats.averageDifficulty = completedCount > 0 ?
                (double) totalDifficultyXP / completedCount : 0;
    }

    private void calculateLast7DaysXP(List<Task> tasks, Statistics stats) {
        Calendar today = Calendar.getInstance();
        Map<String, Integer> xpByDate = new HashMap<>();

        // Inicijalizuj poslednjih 7 dana sa 0 XP
        for (int i = 6; i >= 0; i--) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String dateKey = String.format("%02d/%02d",
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
            xpByDate.put(dateKey, 0);
        }

        // Dodaj XP za završene taskove
        for (Task task : tasks) {
            if (task.getStatus() != Task.TaskStatus.DONE || task.getExecutionTime() == null)
                continue;

            Calendar taskCal = Calendar.getInstance();
            taskCal.setTime(task.getExecutionTime().toDate());

            long diff = today.getTimeInMillis() - taskCal.getTimeInMillis();
            long daysDiff = diff / (1000 * 60 * 60 * 24);

            if (daysDiff >= 0 && daysDiff < 7) {
                String dateKey = String.format("%02d/%02d",
                        taskCal.get(Calendar.MONTH) + 1,
                        taskCal.get(Calendar.DAY_OF_MONTH));

                int xp = task.getTotalXP();
                xpByDate.put(dateKey, xpByDate.getOrDefault(dateKey, 0) + xp);
            }
        }

        // Konvertuj u listu sortirano po datumu
        List<String> sortedDates = new ArrayList<>(xpByDate.keySet());
        Collections.sort(sortedDates, (d1, d2) -> {
            String[] parts1 = d1.split("/");
            String[] parts2 = d2.split("/");
            int month1 = Integer.parseInt(parts1[0]);
            int day1 = Integer.parseInt(parts1[1]);
            int month2 = Integer.parseInt(parts2[0]);
            int day2 = Integer.parseInt(parts2[1]);

            if (month1 != month2) return Integer.compare(month1, month2);
            return Integer.compare(day1, day2);
        });

        for (String date : sortedDates) {
            stats.last7DaysXP.add(new DailyXP(date, xpByDate.get(date)));
        }
    }
}