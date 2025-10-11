package com.example.bossapp.presentation.base;

import android.content.Intent;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.bossapp.MainActivity;
import com.example.bossapp.R;  // ← DODAJ OVO
import com.example.bossapp.business.AuthManager;
import com.example.bossapp.presentation.auth.LoginActivity;
import com.example.bossapp.presentation.notifications.NotificationsFragment;
import com.google.android.material.appbar.MaterialToolbar;

public abstract class BaseFragment extends Fragment {

    protected AuthManager authManager;

    protected void setupToolbar(View view, int toolbarId) {
        MaterialToolbar toolbar = view.findViewById(toolbarId);
        if (toolbar == null) return;

        authManager = new AuthManager(requireContext());

        toolbar.setNavigationOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateBack();
            }
        });

        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_notifications) {
                openNotifications();
                return true;
            } else if (itemId == R.id.action_logout) {
                showLogoutConfirmationDialog();
                return true;
            }
            return false;
        });
    }

    private void openNotifications() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new NotificationsFragment())
                .addToBackStack(null)
                .commit();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        authManager.signOut();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}