package com.example.bossapp.data.repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.bossapp.data.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private static final String COLLECTION_TASKS = "tasks";
    private final FirebaseFirestore db;

    public TaskRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnTaskSaveListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnTasksLoadListener {
        void onSuccess(List<Task> tasks);
        void onError(Exception e);
    }

    public interface OnTaskLoadListener {
        void onSuccess(Task task);
        void onError(Exception e);
    }

    public interface OnTaskDeleteListener {
        void onSuccess();
        void onError(Exception e);
    }

    public void saveTask(Task task, OnTaskSaveListener listener) {
        String id = task.getId() != null ? task.getId() : db.collection(COLLECTION_TASKS).document().getId();
        task.setId(id);

        db.collection(COLLECTION_TASKS)
                .document(id)
                .set(task.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task saved successfully");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving task", e);
                    listener.onError(e);
                });
    }

    public void getTasksByUser(String userId, OnTasksLoadListener listener) {
        db.collection(COLLECTION_TASKS)
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId());
                        tasks.add(task);
                    }
                    listener.onSuccess(tasks);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getTaskById(String taskId, OnTaskLoadListener listener) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Task task = doc.toObject(Task.class);
                        task.setId(doc.getId());
                        listener.onSuccess(task);
                    } else {
                        listener.onError(new Exception("Task not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void deleteTask(String taskId, OnTaskDeleteListener listener) {
        db.collection(COLLECTION_TASKS)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted successfully");
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }
}
