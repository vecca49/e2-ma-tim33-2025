package com.example.bossapp.presentation.task;


import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bossapp.R;
import com.example.bossapp.business.TaskManager;
import com.example.bossapp.data.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;

public class RepeatingTasksFragment extends Fragment {
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> tasks = new ArrayList<>();
    private TaskManager taskManager;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_list, container, false);
        recyclerView = v.findViewById(R.id.recyclerTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        taskAdapter = new TaskAdapter(tasks);
        recyclerView.setAdapter(taskAdapter);

        taskManager = new TaskManager();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadTasks();
        return v;
    }

    public void loadTasks() {
        taskManager.getUserTasks(userId, new TaskManager.OnTasksLoadListener() {
            @Override
            public void onSuccess(List<Task> loadedTasks) {
                tasks.clear();
                for (Task t : loadedTasks) {
                    if (t.isRepeating()) tasks.add(t);
                }
                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

