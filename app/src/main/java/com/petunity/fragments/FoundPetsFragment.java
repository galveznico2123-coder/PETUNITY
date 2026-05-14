package com.petunity.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.petunity.R;
import com.petunity.activities.ChatActivity;
import com.petunity.adapters.LostFoundAdapter;
import com.petunity.models.PetListing;
import com.petunity.viewmodels.LostFoundViewModel;

import java.util.ArrayList;
import java.util.List;

public class FoundPetsFragment extends Fragment {
    private LostFoundAdapter adapter;
    private final List<PetListing> foundPetsList = new ArrayList<>();
    private LostFoundViewModel viewModel;
    private ProgressBar loadingBar;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_found_pets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LostFoundViewModel.class);
        
        RecyclerView recyclerView = view.findViewById(R.id.foundPetsRecyclerView);
        loadingBar = view.findViewById(R.id.loadingProgressBar);
        emptyText = view.findViewById(R.id.emptyStateText);
        
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new LostFoundAdapter(foundPetsList, pet -> {
                if (!isAdded()) return;
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra("other_user_id", pet.getUserId());
                intent.putExtra("user_name", pet.getOwnerName());
                intent.putExtra("avatar_url", pet.getAvatarUrl());
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }

        observeViewModel();
        viewModel.fetchPets("found");
    }

    private void observeViewModel() {
        viewModel.getFoundPets().observe(getViewLifecycleOwner(), pets -> {
            foundPetsList.clear();
            foundPetsList.addAll(pets);
            adapter.notifyDataSetChanged();
            if (emptyText != null) emptyText.setVisibility(pets.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (loadingBar != null) loadingBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
