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
        equipmentRepository.getActiveEquipment(userId, new EquipmentRepository.OnEquipmentListListener() {
            @Override
            public void onSuccess(List<Equipment> equipmentList) {
                for (Equipment equipment : equipmentList) {
                    if (equipment.getRemainingDuration() > 0) {
                        equipment.setRemainingDuration(equipment.getRemainingDuration() - 1);

                        if (equipment.getRemainingDuration() == 0) {
                            equipment.setIsActive(false);
                            Log.d(TAG, equipment.getDisplayName() + " expired");
                        }

                        equipmentRepository.updateEquipment(equipment,
                                new EquipmentRepository.OnEquipmentListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Duration updated for " + equipment.getDisplayName());
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Log.e(TAG, "Error updating duration: " + message);
                                    }
                                });
                    }
                }
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
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