package com.example.bossapp.presentation.alliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.Alliance;
import com.example.bossapp.data.model.MemberProgress;
import com.example.bossapp.data.model.User;
import com.example.bossapp.data.repository.AllianceRepository;
import com.example.bossapp.data.repository.UserRepository;
import com.example.bossapp.presentation.base.BaseFragment;
import com.example.bossapp.presentation.friends.FindFriendsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllianceFragment extends BaseFragment {

    private View viewNoAlliance;
    private View viewHasAlliance;
    private ProgressBar progressBar;

    // No alliance views
    private MaterialButton btnCreateAlliance;

    // Has alliance views
    private TextView tvAllianceName;
    private TextView tvLeaderName;
    private TextView tvMissionProgress;

    private MaterialButton btnStartSpecialMission;
    private TextView tvMemberCount;
    private RecyclerView rvMembers;
    private MaterialButton btnOpenChat;
    private MaterialButton btnInviteFriends;
    private MaterialButton btnLeaveAlliance;
    private MaterialButton btnDisbandAlliance;
    private RecyclerView rvMissionMembersProgress;
    private MissionProgressAdapter missionProgressAdapter;
    private List<MemberProgress> missionProgressList = new ArrayList<>();


    private AllianceRepository allianceRepository;
    private UserRepository userRepository;
    private String currentUserId;
    private User currentUser;
    private Alliance currentAlliance;

    private List<User> membersList = new ArrayList<>();
    private FindFriendsAdapter membersAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alliance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar(view, R.id.toolbar);
        initViews(view);

        allianceRepository = new AllianceRepository();
        userRepository = new UserRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupButtons();
        loadCurrentUser();
    }

    private void initViews(View view) {
        viewNoAlliance = view.findViewById(R.id.viewNoAlliance);
        viewHasAlliance = view.findViewById(R.id.viewHasAlliance);
        progressBar = view.findViewById(R.id.progressBar);

        btnCreateAlliance = view.findViewById(R.id.btnCreateAlliance);

        btnStartSpecialMission = view.findViewById(R.id.btnStartSpecialMission);
        tvMissionProgress = view.findViewById(R.id.tvMissionProgress);
        rvMissionMembersProgress = view.findViewById(R.id.rvMissionMembersProgress);

        tvAllianceName = view.findViewById(R.id.tvAllianceName);
        tvLeaderName = view.findViewById(R.id.tvLeaderName);
        tvMemberCount = view.findViewById(R.id.tvMemberCount);
        rvMembers = view.findViewById(R.id.rvMembers);
        btnOpenChat = view.findViewById(R.id.btnOpenChat);
        btnInviteFriends = view.findViewById(R.id.btnInviteFriends);
        btnLeaveAlliance = view.findViewById(R.id.btnLeaveAlliance);
        btnDisbandAlliance = view.findViewById(R.id.btnDisbandAlliance);
    }


    private void setupButtons() {
        btnCreateAlliance.setOnClickListener(v -> showCreateAllianceDialog());
        btnOpenChat.setOnClickListener(v -> openAllianceChat());
        btnInviteFriends.setOnClickListener(v -> openInviteFriendsScreen());
        btnLeaveAlliance.setOnClickListener(v -> showLeaveAllianceDialog());
        btnDisbandAlliance.setOnClickListener(v -> showDisbandAllianceDialog());
    }

    private void loadCurrentUser() {
        progressBar.setVisibility(View.VISIBLE);

        userRepository.getUserById(currentUserId, new UserRepository.OnUserLoadListener() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                loadUserAlliance();
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserAlliance() {
        allianceRepository.getUserAlliance(currentUserId, new AllianceRepository.OnAllianceLoadListener() {
            @Override
            public void onSuccess(Alliance alliance) {
                progressBar.setVisibility(View.GONE);
                currentAlliance = alliance;

                if (alliance == null) {
                    showNoAllianceView();
                } else {
                    showAllianceView(alliance);
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error loading alliance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoAllianceView() {
        viewNoAlliance.setVisibility(View.VISIBLE);
        viewHasAlliance.setVisibility(View.GONE);
    }

    private void showAllianceView(Alliance alliance) {
        viewNoAlliance.setVisibility(View.GONE);
        viewHasAlliance.setVisibility(View.VISIBLE);

        tvAllianceName.setText(alliance.getAllianceName());
        tvLeaderName.setText("Leader: " + alliance.getLeaderUsername());
        tvMemberCount.setText(alliance.getMemberCount() + " members");

        // Show/hide buttons based on role
        boolean isLeader = alliance.isLeader(currentUserId);
        btnDisbandAlliance.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        btnLeaveAlliance.setVisibility(isLeader ? View.GONE : View.VISIBLE);
        btnInviteFriends.setVisibility(isLeader ? View.VISIBLE : View.GONE);

        btnStartSpecialMission.setVisibility(isLeader && alliance.getCurrentMissionId() == null
                ? View.VISIBLE : View.GONE);

        btnStartSpecialMission.setOnClickListener(v -> startSpecialMission());

        setupMembersRecyclerView();
        loadMembers(alliance.getMemberIds());

        setupMissionProgressRecyclerView();

        String missionId = alliance.getCurrentMissionId();
        if (missionId != null && !missionId.isEmpty()) {
            trackMissionProgress(missionId);
        }
    }

    private void setupMissionProgressRecyclerView() {
        missionProgressAdapter = new MissionProgressAdapter(missionProgressList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.VERTICAL, false);
        rvMissionMembersProgress.setLayoutManager(layoutManager);
        rvMissionMembersProgress.setAdapter(missionProgressAdapter);
    }


    private void startSpecialMission() {
        if (currentAlliance == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Pokreni specijalnu misiju")
                .setMessage("Da li želite da pokrenete specijalnu misiju? Misija traje 2 nedelje i ne može se prekinuti.")
                .setPositiveButton("Pokreni", (dialog, which) -> {
                    performStartSpecialMission();
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void performStartSpecialMission() {
        progressBar.setVisibility(View.VISIBLE);

        allianceRepository.startSpecialMission(
                currentAlliance.getAllianceId(),
                new AllianceRepository.OnAllianceActionListener() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Specijalna misija pokrenuta!", Toast.LENGTH_SHORT).show();

                        loadCurrentUser();
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void trackMissionProgress(String missionId) {
        allianceRepository.getMissionProgress(missionId, new AllianceRepository.OnMissionProgressListener() {
            @Override
            public void onProgress(Map<String, Map<String, Object>> membersProgress, int totalDamage, int bossHp) {
                tvMissionProgress.setText("Boss HP: " + bossHp + " | Ukupna šteta: " + totalDamage);

                missionProgressList.clear();
                for (String memberId : membersProgress.keySet()) {
                    Map<String, Object> data = membersProgress.get(memberId);
                    String username = (String) data.get("username");
                    int storePurchases = data.get("storePurchases") != null ? ((Long) data.get("storePurchases")).intValue() : 0;
                    int bossHits = data.get("bossHits") != null ? ((Long) data.get("bossHits")).intValue() : 0;
                    int easyTasks = data.get("easyTasks") != null ? ((Long) data.get("easyTasks")).intValue() : 0;
                    int otherTasks = data.get("otherTasks") != null ? ((Long) data.get("otherTasks")).intValue() : 0;
                    boolean noUnfinishedTasks = data.get("noUnfinishedTasks") != null && (Boolean) data.get("noUnfinishedTasks");
                    int messageDays = data.get("messageDays") != null ? ((Long) data.get("messageDays")).intValue() : 0;


                    MemberProgress memberProgress = new MemberProgress(
                            username, storePurchases, bossHits, easyTasks,
                            otherTasks, noUnfinishedTasks, messageDays
                    );

                    missionProgressList.add(memberProgress);
                }
                missionProgressAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error loading mission progress", Toast.LENGTH_SHORT).show();
            }
        });
    }





    private void setupMembersRecyclerView() {
        membersAdapter = new FindFriendsAdapter(membersList, null,
                new FindFriendsAdapter.OnUserActionListener() {
                    @Override
                    public void onAddFriend(User user) {}

                    @Override
                    public void onRemoveFriend(User user) {}

                    @Override
                    public void onViewProfile(User user) {
                        // Open profile - implement if needed
                    }
                }, false);

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvMembers.setLayoutManager(layoutManager);
        rvMembers.setAdapter(membersAdapter);
    }

    private void loadMembers(List<String> memberIds) {
        allianceRepository.getAllianceMembers(memberIds, new UserRepository.OnUsersLoadListener() {
            @Override
            public void onSuccess(List<User> users) {
                membersList.clear();
                membersList.addAll(users);
                membersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error loading members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateAllianceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_alliance, null);
        TextInputEditText etAllianceName = dialogView.findViewById(R.id.etAllianceName);

        new AlertDialog.Builder(requireContext())
                .setTitle("Create Alliance")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etAllianceName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter alliance name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createAlliance(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createAlliance(String allianceName) {
        progressBar.setVisibility(View.VISIBLE);

        allianceRepository.createAlliance(allianceName, currentUserId, currentUser.getUsername(),
                new AllianceRepository.OnAllianceActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "Alliance created!", Toast.LENGTH_SHORT).show();
                        loadCurrentUser();
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openAllianceChat() {
        if (currentAlliance == null) return;

        AllianceChatFragment chatFragment = AllianceChatFragment.newInstance(
                currentAlliance.getAllianceId(),
                currentAlliance.getAllianceName());

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, chatFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openInviteFriendsScreen() {
        if (currentAlliance == null) return;

        InviteFriendsFragment fragment = InviteFriendsFragment.newInstance(
                currentAlliance.getAllianceId(),
                currentAlliance.getAllianceName());

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showLeaveAllianceDialog() {
        if (currentAlliance == null) return;

        if (currentAlliance.getCurrentMissionId() != null &&
                !currentAlliance.getCurrentMissionId().isEmpty()) {
            Toast.makeText(requireContext(),
                    "Cannot leave alliance during active mission",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Leave Alliance")
                .setMessage("Are you sure you want to leave " + currentAlliance.getAllianceName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> leaveAlliance())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveAlliance() {
        if (currentAlliance == null) return;

        progressBar.setVisibility(View.VISIBLE);

        allianceRepository.leaveAlliance(currentUserId, currentAlliance.getAllianceId(),
                new AllianceRepository.OnAllianceActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "You left the alliance", Toast.LENGTH_SHORT).show();
                        loadCurrentUser();
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDisbandAllianceDialog() {
        if (currentAlliance == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Disband Alliance")
                .setMessage("Are you sure you want to disband " + currentAlliance.getAllianceName() +
                        "? The alliance will be PERMANENTLY DELETED and all members will be removed.")
                .setPositiveButton("Yes, Disband", (dialog, which) -> disbandAlliance())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void disbandAlliance() {
        if (currentAlliance == null) return;

        progressBar.setVisibility(View.VISIBLE);

        allianceRepository.disbandAllianceCompletely(
                currentAlliance.getAllianceId(),
                currentAlliance.getMemberIds(),
                new AllianceRepository.OnAllianceActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "Alliance permanently disbanded", Toast.LENGTH_SHORT).show();
                        loadCurrentUser();
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCurrentUser();
    }
}