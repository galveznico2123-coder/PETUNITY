package com.petunity.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.petunity.R;
import com.petunity.activities.ReportLostPetActivity;
import com.petunity.adapters.LostFoundPagerAdapter;

public class FindFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_find, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewPager2 viewPager = view.findViewById(R.id.viewPager);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        FloatingActionButton addPetFab = view.findViewById(R.id.addPetFab);

        if (viewPager != null && tabLayout != null) {
            viewPager.setAdapter(new LostFoundPagerAdapter(this));

            new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> tab.setText(position == 0 ? "Lost Pets" : "Found Pets")
            ).attach();
        }

        if (addPetFab != null) {
            addPetFab.setOnClickListener(v -> {
                // Determine if we are on the Lost (0) or Found (1) tab
                boolean isLostTab = (viewPager != null) && (viewPager.getCurrentItem() == 0);
                
                Intent intent = new Intent(requireContext(), ReportLostPetActivity.class);
                intent.putExtra("is_lost", isLostTab);
                startActivity(intent);
            });
        }
    }
}
