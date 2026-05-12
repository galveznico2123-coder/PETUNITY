package com.petunity.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.petunity.fragments.FoundPetsFragment;
import com.petunity.fragments.LostPetsFragment;

public class LostFoundPagerAdapter extends FragmentStateAdapter {
    public LostFoundPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new LostPetsFragment();
        } else {
            return new FoundPetsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}