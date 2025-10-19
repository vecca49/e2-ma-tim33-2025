package com.example.bossapp.data.model;

import java.util.HashMap;
import java.util.Map;

public class Equipment {

    public enum EquipmentType {
        POTION,
        ARMOR,    // (Gloves, Shield, Boots)
        WEAPON    // (Sword, Bow)
    }

    public enum PotionType {
        POWER_20_TEMP("Power +20% (1 battle)", 0.20, true, 1),
        POWER_40_TEMP("Power +40% (1 battle)", 0.40, true, 1),
        POWER_5_PERM("Power +5% (Permanent)", 0.05, false, -1),
        POWER_10_PERM("Power +10% (Permanent)", 0.10, false, -1);

        public final String displayName;
        public final double powerBoost;
        public final boolean isTemporary;
        public final int duration; // -1 = permanent

        PotionType(String displayName, double powerBoost, boolean isTemporary, int duration) {
            this.displayName = displayName;
            this.powerBoost = powerBoost;
            this.isTemporary = isTemporary;
            this.duration = duration;
        }
    }

    public enum ArmorType {
        GLOVES("Gloves", 0.10, "powerBoost", 2),
        SHIELD("Shield", 0.10, "successRate", 2),
        BOOTS("Boots", 0.40, "extraAttack", 2);

        public final String displayName;
        public final double value;
        public final String bonusType; // powerBoost, successRate, extraAttack
        public final int duration; // number of boss fights

        ArmorType(String displayName, double value, String bonusType, int duration) {
            this.displayName = displayName;
            this.value = value;
            this.bonusType = bonusType;
            this.duration = duration;
        }
    }

    public enum WeaponType {
        SWORD("Sword", 0.05, "powerBoost"),
        BOW("Bow", 0.05, "coinBoost");

        public final String displayName;
        public final double baseValue;
        public final String bonusType;

        WeaponType(String displayName, double baseValue, String bonusType) {
            this.displayName = displayName;
            this.baseValue = baseValue;
            this.bonusType = bonusType;
        }
    }

    private String equipmentId;
    private String userId;
    private EquipmentType type;
    private String subType; // POWER_20_TEMP, GLOVES, SWORD...
    private int quantity; // For potione
    private boolean isActive;
    private int remainingDuration; // For armor and temp potions
    private int upgradeLevel; // For weapons
    private double currentValue; // Current value (can be increased)
    private long acquiredAt;

    public Equipment() {}

    // Potion Constructor
    public Equipment(String userId, PotionType potionType) {
        this.equipmentId = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = EquipmentType.POTION;
        this.subType = potionType.name();
        this.quantity = 1;
        this.isActive = false;
        this.remainingDuration = potionType.duration;
        this.currentValue = potionType.powerBoost;
        this.acquiredAt = System.currentTimeMillis();
    }

    // Armor Constructor
    public Equipment(String userId, ArmorType armorType) {
        this.equipmentId = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = EquipmentType.ARMOR;
        this.subType = armorType.name();
        this.quantity = 1;
        this.isActive = false;
        this.remainingDuration = armorType.duration;
        this.currentValue = armorType.value;
        this.acquiredAt = System.currentTimeMillis();
    }

    // Weapon Constructor
    public Equipment(String userId, WeaponType weaponType) {
        this.equipmentId = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = EquipmentType.WEAPON;
        this.subType = weaponType.name();
        this.quantity = 1;
        this.isActive = false;
        this.remainingDuration = -1; // permanent
        this.upgradeLevel = 0;
        this.currentValue = weaponType.baseValue;
        this.acquiredAt = System.currentTimeMillis();
    }

    // Pricing by user level
    public static int calculatePrice(EquipmentType type, String subType, int userLevel) {
        // Reward for defeating the boss at the end of the PREVIOUS level
        int baseReward = (userLevel == 0) ? 200 : (int) (200 * Math.pow(1.2, userLevel - 1));

        switch (type) {
            case POTION:
                PotionType potion = PotionType.valueOf(subType);
                switch (potion) {
                    case POWER_20_TEMP: return (int) (baseReward * 0.5);
                    case POWER_40_TEMP: return (int) (baseReward * 0.7);
                    case POWER_5_PERM: return baseReward * 2;
                    case POWER_10_PERM: return baseReward * 10;
                }
                break;

            case ARMOR:
                ArmorType armor = ArmorType.valueOf(subType);
                switch (armor) {
                    case GLOVES:
                    case SHIELD:
                        return (int) (baseReward * 0.6);
                    case BOOTS:
                        return (int) (baseReward * 0.8);
                }
                break;

            case WEAPON:
                return (int) (baseReward * 0.6); // upgrade price
        }

        return 100;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("equipmentId", equipmentId);
        map.put("userId", userId);
        map.put("type", type.name());
        map.put("subType", subType);
        map.put("quantity", quantity);
        map.put("isActive", isActive);
        map.put("remainingDuration", remainingDuration);
        map.put("upgradeLevel", upgradeLevel);
        map.put("currentValue", currentValue);
        map.put("acquiredAt", acquiredAt);
        return map;
    }

    // Getters & Setters
    public String getEquipmentId() { return equipmentId; }
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public EquipmentType getType() { return type; }
    public void setType(EquipmentType type) { this.type = type; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }

    public int getRemainingDuration() { return remainingDuration; }
    public void setRemainingDuration(int remainingDuration) {
        this.remainingDuration = remainingDuration;
    }

    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int upgradeLevel) { this.upgradeLevel = upgradeLevel; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public long getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(long acquiredAt) { this.acquiredAt = acquiredAt; }

    public String getDisplayName() {
        switch (type) {
            case POTION:
                return PotionType.valueOf(subType).displayName;
            case ARMOR:
                return ArmorType.valueOf(subType).displayName;
            case WEAPON:
                return WeaponType.valueOf(subType).displayName +
                        (upgradeLevel > 0 ? " +" + upgradeLevel : "");
        }
        return "Unknown";
    }
}