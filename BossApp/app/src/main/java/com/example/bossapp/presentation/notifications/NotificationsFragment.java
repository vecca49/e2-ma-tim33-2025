package com.example.bossapp.presentation.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.FriendRequest;
import com.example.bossapp.data.repository.FriendRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.friends.FriendRequestAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends BaseFragment {

    private RecyclerView rvNotifications;
    private LinearLayout tvEmptyState;
    private FriendRequestAdapter adapter;
    private List<FriendRequest> requestList = new ArrayList<>();
    private FriendRepository friendRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);
        friendRepository = new FriendRepository();
        setupRecyclerView();
        loadFriendRequests();
    }

    private void initViews(View view) {
        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupRecyclerView() {
        adapter = new FriendRequestAdapter(requestList, new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(FriendRequest request) {
                handleAccept(request);
            }

            @Override
            public void onReject(FriendRequest request) {
                handleReject(request);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);
    }

    private void loadFriendRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        friendRepository.getPendingRequests(currentUserId,
                new FriendRepository.OnFriendRequestsLoadListener() {
                    @Override
                    public void onSuccess(List<FriendRequest> requests) {
                        requestList.clear();
                        requestList.addAll(requests);
                        adapter.notifyDataSetChanged();

                        if (requests.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            rvNotifications.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            rvNotifications.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error loading requests: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleAccept(FriendRequest request) {
        friendRepository.acceptFriendRequest(
                request.getRequestId(),
                request.getSenderId(),
                request.getReceiverId(),
                new FriendRepository.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Friend request accepted!",
                                Toast.LENGTH_SHORT).show();
                        loadFriendRequests(); // Reload
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleReject(FriendRequest request) {
        friendRepository.rejectFriendRequest(
                request.getRequestId(),
                new FriendRepository.OnFriendRequestListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Friend request rejected",
                                Toast.LENGTH_SHORT).show();
                        loadFriendRequests(); // Reload
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}