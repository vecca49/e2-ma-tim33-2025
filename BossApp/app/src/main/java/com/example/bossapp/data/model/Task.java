package com.example.bossapp.data.model;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Task {

    private String id;
    private String name;
    private String description;
    private String categoryId;
    private String categoryName;
    private String categoryColor;

    private boolean repeating;
    private int repeatInterval;
    private RepeatUnit repeatUnit;
    private Timestamp startDate;
    private Timestamp endDate;
    private Timestamp executionTime;

    private Difficulty difficulty;
    private Importance importance;
    private int totalXP;

    private TaskStatus status;
    private String ownerId;

    private boolean xpAwarded = false;

    public Task() {}

    public Task(String name,
                String description,
                String categoryId,
                String categoryName,
                String categoryColor,
                boolean repeating,
                int repeatInterval,
                RepeatUnit repeatUnit,
                Timestamp startDate,
                Timestamp endDate,
                Timestamp executionTime,
                Difficulty difficulty,
                Importance importance,
                int totalXP,
                TaskStatus status,
                String ownerId) {

        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.repeating = repeating;
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.executionTime = executionTime;
        this.difficulty = difficulty;
        this.importance = importance;
        this.totalXP = totalXP;
        this.status = status;
        this.ownerId = ownerId;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("categoryId", categoryId);
        map.put("categoryName", categoryName);
        map.put("categoryColor", categoryColor);
        map.put("repeating", repeating);
        map.put("repeatInterval", repeatInterval);
        map.put("repeatUnit", repeatUnit != null ? repeatUnit.name() : null);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("executionTime", executionTime);
        map.put("difficulty", difficulty != null ? difficulty.name() : null);
        map.put("importance", importance != null ? importance.name() : null);
        map.put("totalXP", totalXP);
        map.put("status", status != null ? status.name() : null);
        map.put("ownerId", ownerId);
        map.put("xpAwarded", xpAwarded);
        return map;
    }


    public enum Difficulty {
        VERY_EASY(1),
        EASY(3),
        HARD(7),
        EXTREME(20);

        private final int xp;

        Difficulty(int xp) {
            this.xp = xp;
        }

        public int getXp() {
            return xp;
        }
    }

    public enum Importance {
        NORMAL(1),
        IMPORTANT(3),
        VERY_IMPORTANT(10),
        SPECIAL(100);

        private final int xp;

        Importance(int xp) {
            this.xp = xp;
        }

        public int getXp() {
            return xp;
        }
    }

    public enum RepeatUnit {
        DAY,
        WEEK
    }

    public enum TaskStatus {
        ACTIVE,
        DONE,
        NOT_DONE,
        PAUSED,
        CANCELED
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryColor() { return categoryColor; }
    public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }

    public boolean isRepeating() { return repeating; }
    public void setRepeating(boolean repeating) { this.repeating = repeating; }

    public int getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(int repeatInterval) { this.repeatInterval = repeatInterval; }

    public RepeatUnit getRepeatUnit() { return repeatUnit; }
    public void setRepeatUnit(RepeatUnit repeatUnit) { this.repeatUnit = repeatUnit; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }

    public Timestamp getExecutionTime() { return executionTime; }
    public void setExecutionTime(Timestamp executionTime) { this.executionTime = executionTime; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public Importance getImportance() { return importance; }
    public void setImportance(Importance importance) { this.importance = importance; }

    public int getTotalXP() { return totalXP; }
    public void setTotalXP(int totalXP) { this.totalXP = totalXP; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public boolean canChangeStatus() {
        return status == TaskStatus.ACTIVE || status == TaskStatus.PAUSED;
    }

    public boolean isXpAwarded() {
        return xpAwarded;
    }

    public void setXpAwarded(boolean xpAwarded) {
        this.xpAwarded = xpAwarded;
    }

    public boolean isWithinQuota() {
        return true;
    }
}

