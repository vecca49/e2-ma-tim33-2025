package com.example.bossapp.presentation.alliance;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.AllianceMessage;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.AllianceChatRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AllianceChatFragment extends BaseFragment {

    private static final String TAG = "AllianceChatFragment";  // ← OVO JE VAŽNO!
    private static final String ARG_ALLIANCE_ID = "allianceId";
    private static final String ARG_ALLIANCE_NAME = "allianceName";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;

    private AllianceChatAdapter adapter;
    private List<AllianceMessage> messageList = new ArrayList<>();

    private AllianceChatRepository chatRepository;
    private UserRepository userRepository;
    private String allianceId;
    private String allianceName;
    private String currentUserId;
    private User currentUser;

    private ListenerRegistration messageListener;

    public static AllianceChatFragment newInstance(String allianceId, String allianceName) {
        AllianceChatFragment fragment = new AllianceChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ALLIANCE_ID, allianceId);
        args.putString(ARG_ALLIANCE_NAME, allianceName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            allianceId = getArguments().getString(ARG_ALLIANCE_ID);
            allianceName = getArguments().getString(ARG_ALLIANCE_NAME);
        }

        Log.d(TAG, "Alliance Chat opened for: " + allianceName + " (ID: " + allianceId + ")");

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        chatRepository = new AllianceChatRepository();
        userRepository = new UserRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupRecyclerView();
        loadCurrentUser();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messageListener != null) {
            messageListener.remove();
            Log.d(TAG, "Message listener removed");
        }
    }

    private void initViews(View view) {
        rvMessages = view.findViewById(R.id.rvMessages);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new AllianceChatAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        // Scroll to bottom when keyboard opens
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && messageList.size() > 0) {
                rvMessages.postDelayed(() ->
                        rvMessages.smoothScrollToPosition(messageList.size() - 1), 100);
            }
        });
    }

    private void loadCurrentUser() {
        progressBar.setVisibility(View.VISIBLE);

        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                progressBar.setVisibility(View.GONE);

                Log.d(TAG, "Current user loaded: " + user.getUsername());

                btnSend.setOnClickListener(v -> sendMessage());
                startListeningToMessages();
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading user data: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startListeningToMessages() {
        Log.d(TAG, "Starting to listen to messages for alliance: " + allianceId);

        messageListener = chatRepository.listenToMessages(allianceId, (querySnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error loading messages: " + error.getMessage(), error);
                Toast.makeText(requireContext(),
                        "Error loading messages: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (querySnapshot != null) {
                Log.d(TAG, "Received " + querySnapshot.size() + " messages");

                messageList.clear();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    AllianceMessage msg = doc.toObject(AllianceMessage.class);
                    if (msg != null) {
                        Log.d(TAG, "Message: " + msg.getSenderUsername() + " - " + msg.getMessageText());
                        messageList.add(msg);
                    }
                }
                adapter.notifyDataSetChanged();

                if (messageList.size() > 0) {
                    rvMessages.smoothScrollToPosition(messageList.size() - 1);
                }
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(requireContext(), "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Sending message: " + messageText);

        AllianceMessage message = new AllianceMessage(
                allianceId,
                currentUserId,
                currentUser.getUsername(),
                currentUser.getAvatarIndex(),
                messageText
        );

        btnSend.setEnabled(false);

        chatRepository.sendMessage(message, new AllianceChatRepository.OnMessageSendListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Message sent successfully");
                etMessage.setText("");
                btnSend.setEnabled(true);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error sending message: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Error sending message: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                btnSend.setEnabled(true);
            }
        });
    }
}