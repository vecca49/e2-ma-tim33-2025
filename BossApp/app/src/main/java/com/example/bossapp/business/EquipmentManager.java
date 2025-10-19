package com.example.bossapp.business;

import android.util.Log;

import com.example.bossapp.data.model.Equipment;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.EquipmentRepository;
import com.example.bossapp.data.repository.UserRepository;

import java.util.List;

public class EquipmentManager {

    private static final String TAG = "EquipmentManager";
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    public EquipmentManager() {
        this.equipmentRepository = new EquipmentRepository();
        this.userRepository = new UserRepository();
    }

    public interface OnPurchaseListener {
        void onSuccess(String message);
        void onError(String message);
    }

    // Buy equipment
    public void purchaseEquipment(User user, Equipment.EquipmentType type, String subType,
                                  OnPurchaseListener listener) {

        int price = Equipment.calculatePrice(type, subType, user.getLevel());

        if (user.getCoins() < price) {
            listener.onError("Not enough coins! Need " + price + ", have " + user.getCoins());
            return;
        }

        // Create equipment
        Equipment equipment = null;
        switch (type) {
            case POTION:
                equipment = new Equipment(user.getUserId(), Equipment.PotionType.valueOf(subType));
                break;
            case ARMOR:
                equipment = new Equipment(user.getUserId(), Equipment.ArmorType.valueOf(subType));
                break;
            case WEAPON:
                equipment = new Equipment(user.getUserId(), Equipment.WeaponType.valueOf(subType));
                break;
        }

        if (equipment == null) {
            listener.onError("Invalid equipment type");
            return;
        }

        Equipment finalEquipment = equipment;

        // Add equipment to the database
        equipmentRepository.addEquipment(equipment, new EquipmentRepository.OnEquipmentListener() {
            @Override
            public void onSuccess() {
                // Subtract coins
                user.setCoins(user.getCoins() - price);

                userRepository.saveUser(user, new UserRepository.OnUserSaveListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Purchase successful: " + finalEquipment.getDisplayName());
                        listener.onSuccess("Purchased: " + finalEquipment.getDisplayName());
                    }

                    @Override
                    public void onError(Exception e) {
                        listener.onError("Error saving user: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                listener.onError("Error adding equipment: " + message);
            }
        });
    }

    public void activateEquipment(Equipment equipment, OnPurchaseListener listener) {
        equipment.setIsActive(true);

        equipmentRepository.updateEquipment(equipment, new EquipmentRepository.OnEquipmentListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Equipment activated: " + equipment.getDisplayName());
                listener.onSuccess("Activated: " + equipment.getDisplayName());
            }

            @Override
            public void onError(String message) {
                listener.onError("Error activating equipment: " + message);
            }
        });
    }


    public void deactivateEquipment(Equipment equipment, OnPurchaseListener listener) {
        equipment.setIsActive(false);

        equipmentRepository.updateEquipment(equipment, new EquipmentRepository.OnEquipmentListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Equipment deactivated: " + equipment.getDisplayName());
                listener.onSuccess("Deactivated: " + equipment.getDisplayName());
            }

            @Override
            public void onError(String message) {
                listener.onError("Error deactivating equipment: " + message);
            }
        });
    }

    // Decrease duration after boss fight
    public void processFightEnd(String userId, EquipmentRepository.OnEquipmentListener listener) {
        Log.d(TAG, "🔵 =======================================");
        Log.d(TAG, "🔵 PROCESS FIGHT END CALLED");
        Log.d(TAG, "🔵 User ID: " + userId);
        Log.d(TAG, "🔵 =======================================");

        equipmentRepository.getActiveEquipment(userId, new EquipmentRepository.OnEquipmentListListener() {
            @Override
            public void onSuccess(List<Equipment> equipmentList) {
                if (equipmentList.isEmpty()) {
                    Log.d(TAG, "No active equipment to process");
                    listener.onSuccess();
                    return;
                }

                final int[] processedCount = {0};
                final int totalEquipment = equipmentList.size();

                for (Equipment equipment : equipmentList) {
                    // CHECK IF THERE IS DURATION (temp potions and armor have them, weapons don't)
                    if (equipment.getRemainingDuration() > 0) {
                        // Decrease duration by 1
                        equipment.setRemainingDuration(equipment.getRemainingDuration() - 1);

                        Log.d(TAG, equipment.getDisplayName() + " duration: " +
                                (equipment.getRemainingDuration() + 1) + " -> " + equipment.getRemainingDuration());

                        // IF DURATION IS NOW 0, DEACTIVATE EQUIPMENT
                        if (equipment.getRemainingDuration() == 0) {
                            equipment.setIsActive(false);
                            Log.d(TAG, "❌ " + equipment.getDisplayName() + " EXPIRED and deactivated!");

                            // -------------------------------
                            // AKO JE TEMP POTION, OBRIŠI GA POTPUNO
                            if (equipment.getType() == Equipment.EquipmentType.POTION) {
                                Equipment.PotionType potion = Equipment.PotionType.valueOf(equipment.getSubType());
                                if (potion.isTemporary) {
                                    Log.d(TAG, "🗑️ Deleting temporary potion: " + equipment.getDisplayName());
                                    equipmentRepository.deleteEquipment(equipment.getEquipmentId(),
                                            new EquipmentRepository.OnEquipmentListener() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "✅ Temp potion deleted successfully");
                                                    processedCount[0]++;
                                                    if (processedCount[0] == totalEquipment) {
                                                        listener.onSuccess();
                                                    }
                                                }

                                                @Override
                                                public void onError(String message) {
                                                    Log.e(TAG, "❌ Error deleting temp potion: " + message);
                                                    processedCount[0]++;
                                                    if (processedCount[0] == totalEquipment) {
                                                        listener.onSuccess();
                                                    }
                                                }
                                            });
                                    continue; // Skip the update, we have already deleted
                                }
                            }
// 🟢 DODATO - OBRIŠI ARMOR NAKON ŠTO DURATION DOSTIGNE 0
                            else if (equipment.getType() == Equipment.EquipmentType.ARMOR) {
                                Log.d(TAG, "🗑️ Deleting expired armor: " + equipment.getDisplayName());
                                equipmentRepository.deleteEquipment(equipment.getEquipmentId(),
                                        new EquipmentRepository.OnEquipmentListener() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d(TAG, "✅ Armor deleted successfully");
                                                processedCount[0]++;
                                                if (processedCount[0] == totalEquipment) {
                                                    listener.onSuccess();
                                                }
                                            }

                                            @Override
                                            public void onError(String message) {
                                                Log.e(TAG, "❌ Error deleting armor: " + message);
                                                processedCount[0]++;
                                                if (processedCount[0] == totalEquipment) {
                                                    listener.onSuccess();
                                                }
                                            }
                                        });
                                continue; // Skip the update, we have already deleted
                            }
                            //----------------------------------------------
                        }

                        // Update equipment in Firestore
                        equipmentRepository.updateEquipment(equipment,
                                new EquipmentRepository.OnEquipmentListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "✅ Duration updated for " + equipment.getDisplayName());
                                        processedCount[0]++;
                                        if (processedCount[0] == totalEquipment) {
                                            listener.onSuccess();
                                        }
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Log.e(TAG, "❌ Error updating duration: " + message);
                                        processedCount[0]++;
                                        if (processedCount[0] == totalEquipment) {
                                            listener.onSuccess();
                                        }
                                    }
                                });

                    } else {
                        // Permanent equipment (weapons, perm potions) - bez duration-a
                        Log.d(TAG, equipment.getDisplayName() + " is permanent, no duration to decrease");
                        processedCount[0]++;
                        if (processedCount[0] == totalEquipment) {
                            listener.onSuccess();
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "❌ Error loading active equipment: " + message);
                listener.onError(message);
            }
        });
    }

    // Upgrade weapon
    public void upgradeWeapon(User user, Equipment weapon, OnPurchaseListener listener) {
        if (weapon.getType() != Equipment.EquipmentType.WEAPON) {
            listener.onError("Only weapons can be upgraded");
            return;
        }

        int upgradeCost = Equipment.calculatePrice(Equipment.EquipmentType.WEAPON,
                weapon.getSubType(), user.getLevel());

        if (user.getCoins() < upgradeCost) {
            listener.onError("Not enough coins for upgrade");
            return;
        }

        // Increase upgrade level and current value
        weapon.setUpgradeLevel(weapon.getUpgradeLevel() + 1);
        weapon.setCurrentValue(weapon.getCurrentValue() + 0.01); // +0.01% per upgrade

        equipmentRepository.updateEquipment(weapon, new EquipmentRepository.OnEquipmentListener() {
            @Override
            public void onSuccess() {
                user.setCoins(user.getCoins() - upgradeCost);

                userRepository.saveUser(user, new UserRepository.OnUserSaveListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Weapon upgraded: " + weapon.getDisplayName());
                        listener.onSuccess("Weapon upgraded to +" + weapon.getUpgradeLevel());
                    }

                    @Override
                    public void onError(Exception e) {
                        listener.onError("Error saving user: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                listener.onError("Error upgrading weapon: " + message);
            }
        });
    }
}