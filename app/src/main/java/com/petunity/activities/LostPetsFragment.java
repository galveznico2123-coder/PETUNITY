package com.petunity.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.adapters.LostFoundAdapter;
import com.petunity.models.PetListing;

import java.util.ArrayList;
import java.util.List;

public class LostPetsFragment extends Fragment {
    private static final String TAG = "LostPetsFragment";
    private LostFoundAdapter adapter;
    private List<PetListing> lostPetsList = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lost_pets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        RecyclerView recyclerView = view.findViewById(R.id.lostPetsRecyclerView);
        
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new LostFoundAdapter(lostPetsList, pet -> {
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra("other_user_id", pet.getOwnerId());
                intent.putExtra("user_name", pet.getOwnerName());
                intent.putExtra("avatar_url", pet.getAvatarUrl());
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
            
            fetchLostPets();
        }
    }

    private void fetchLostPets() {
        db.collection("pet_listing")
                .whereEqualTo("status", "lost")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        lostPetsList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            PetListing pet = doc.toObject(PetListing.class);
                            if (pet != null) {
                                lostPetsList.add(pet);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
