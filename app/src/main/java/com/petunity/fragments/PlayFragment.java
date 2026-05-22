package com.petunity.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.activities.AddPetActivity;
import com.petunity.adapters.MatchesAdapter;
import com.petunity.models.PetProfile;
import com.petunity.viewmodels.PlayViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayFragment extends Fragment implements MatchesAdapter.OnMatchClickListener {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    
    private RecyclerView matchesRecyclerView;
    private MatchesAdapter adapter;
    private LottieAnimationView loadingProgressBar;
    private TextView emptyStateText;
    private View requestsButton, addPetButton;

    private PlayViewModel viewModel;
    private PetProfile myPet;
    private List<PetProfile> matchesList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_play, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(PlayViewModel.class);

        // UI Bindings
        matchesRecyclerView = view.findViewById(R.id.matchesRecyclerView);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        requestsButton = view.findViewById(R.id.requestsButton);
        addPetButton = view.findViewById(R.id.addPetButton);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
        
        viewModel.loadData();
    }

    private void setupRecyclerView() {
        matchesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MatchesAdapter(matchesList, null, this);
        matchesRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        if (requestsButton != null) {
            requestsButton.setOnClickListener(v -> {
                if (!isAdded()) return;
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, new PlaydateRequestsFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (addPetButton != null) {
            addPetButton.setOnClickListener(v -> {
                if (!isAdded()) return;
                // Direct to create persona (AddPetActivity)
                startActivity(new Intent(requireContext(), AddPetActivity.class));
            });
        }
    }

    private void observeViewModel() {
        viewModel.getMyPet().observe(getViewLifecycleOwner(), pet -> {
            this.myPet = pet;
            // Re-initialize adapter with myPet to calculate scores
            adapter = new MatchesAdapter(matchesList, myPet, this);
            matchesRecyclerView.setAdapter(adapter);
        });

        viewModel.getPotentialMatches().observe(getViewLifecycleOwner(), matches -> {
            matchesList.clear();
            matchesList.addAll(matches);
            adapter.notifyDataSetChanged();
            
            if (emptyStateText != null) {
                emptyStateText.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMatchClick(PetProfile profile) {
        showPlaydateRequestModal(profile);
    }

    private void showPlaydateRequestModal(PetProfile match) {
        if (!isAdded() || getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_playdate_request, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        if (title != null) title.setText("Request Playdate with " + match.getName());

        TextInputEditText timeInput = dialogView.findViewById(R.id.timeInput);
        TextInputEditText locationInput = dialogView.findViewById(R.id.locationInput);
        MaterialButton sendButton = dialogView.findViewById(R.id.sendRequestButton);

        if (sendButton != null) {
            sendButton.setOnClickListener(v -> {
                String time = timeInput != null && timeInput.getText() != null ? timeInput.getText().toString() : "";
                String loc = locationInput != null && locationInput.getText() != null ? locationInput.getText().toString() : "";
                
                if (time.isEmpty() || loc.isEmpty()) {
                    Toast.makeText(getContext(), "Please suggest a time and place", Toast.LENGTH_SHORT).show();
                    return;
                }

                sendRequestToFirebase(match, time, loc);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void sendRequestToFirebase(PetProfile match, String time, String loc) {
        if (mAuth.getUid() == null) return;
        Map<String, Object> request = new HashMap<>();
        request.put("senderId", mAuth.getUid());
        request.put("receiverId", match.getOwnerId());
        request.put("senderPetName", myPet != null ? myPet.getName() : "My Pet");
        request.put("receiverPetName", match.getName());
        request.put("suggestedTime", time);
        request.put("suggestedLocation", loc);
        request.put("status", "Pending");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("playdate_requests").add(request)
                .addOnSuccessListener(doc -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Playdate Request Sent!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
