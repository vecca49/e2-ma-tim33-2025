package com.example.bossapp.data.repository;

import android.util.Log;

import com.example.bossapp.data.model.Equipment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EquipmentRepository {

    private static final String TAG = "EquipmentRepository";
    private static final String COLLECTION = "equipment";
    private final FirebaseFirestore db;

    public EquipmentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnEquipmentListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnEquipmentListListener {
        void onSuccess(List<Equipment> equipmentList);
        void onError(String message);
    }

    public void addEquipment(Equipment equipment, OnEquipmentListener listener) {
        db.collection(COLLECTION)
                .document(equipment.getEquipmentId())
                .set(equipment.toMap())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Equipment added: " + equipment.getDisplayName());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding equipment", e);
                    listener.onError(e.getMessage());
                });
    }

    public void getUserEquipment(String userId, OnEquipmentListListener listener) {
        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Equipment> equipmentList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Equipment equipment = doc.toObject(Equipment.class);
                        equipmentList.add(equipment);
                    }
                    Log.d(TAG, "Loaded " + equipmentList.size() + " equipment items");
                    listener.onSuccess(equipmentList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading equipment", e);
                    listener.onError(e.getMessage());
                });
    }

    public void updateEquipment(Equipment equipment, OnEquipmentListener listener) {
        db.collection(COLLECTION)
                .document(equipment.getEquipmentId())
                .set(equipment.toMap())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Equipment updated: " + equipment.getDisplayName());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating equipment", e);
                    listener.onError(e.getMessage());
                });
    }

    public void deleteEquipment(String equipmentId, OnEquipmentListener listener) {
        db.collection(COLLECTION)
                .document(equipmentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Equipment deleted: " + equipmentId);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting equipment", e);
                    listener.onError(e.getMessage());
                });
    }


    public void getActiveEquipment(String userId, OnEquipmentListListener listener) {
        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Equipment> equipmentList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Equipment equipment = doc.toObject(Equipment.class);
                        equipmentList.add(equipment);
                    }
                    Log.d(TAG, "Loaded " + equipmentList.size() + " active equipment items");
                    listener.onSuccess(equipmentList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading active equipment", e);
                    listener.onError(e.getMessage());
                });
    }
}