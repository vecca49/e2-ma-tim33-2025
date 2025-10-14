package com.example.bossapp.presentation.task;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;

public class TaskTabsAdapter extends FragmentStateAdapter {
    private final List<Fragment> fragments;

    public TaskTabsAdapter(@NonNull Fragment parentFragment, List<Fragment> fragments) {
        super(parentFragment);
        this.fragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }
}
