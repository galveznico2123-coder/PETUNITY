package com.petunity.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.activities.AddPetActivity;
import com.petunity.models.PetProfile;
import com.petunity.viewmodels.PlayViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlayFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    
    private ImageView petImageView;
    private TextView petNameText, petMatchScoreText, petBreedTagText, petDescriptionText;
    private MaterialButton matchButton, passButton;
    private View requestsButton, addPetButton;
    private ProgressBar loadingProgressBar;
    private TextView emptyStateText;
    private View petCard;

    private PlayViewModel viewModel;
    private List<PetProfile> potentialMatches = new ArrayList<>();
    private int currentMatchIndex = 0;
    private PetProfile myPet;

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
        petImageView = view.findViewById(R.id.petImage);
        petNameText = view.findViewById(R.id.dogName);
        petMatchScoreText = view.findViewById(R.id.petMatchScore);
        petBreedTagText = view.findViewById(R.id.petBreedTag);
        petDescriptionText = view.findViewById(R.id.petDescription);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        petCard = view.findViewById(R.id.petCard);
        
        matchButton = view.findViewById(R.id.matchButton);
        passButton = view.findViewById(R.id.passButton);
        requestsButton = view.findViewById(R.id.requestsButton);
        addPetButton = view.findViewById(R.id.addPetButton);

        setupClickListeners();
        observeViewModel();
        
        viewModel.loadData();
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
                startActivity(new Intent(requireContext(), AddPetActivity.class));
            });
        }

        if (passButton != null) {
            passButton.setOnClickListener(v -> {
                currentMatchIndex++;
                showNextMatch();
            });
        }
    }

    private void observeViewModel() {
        viewModel.getMyPet().observe(getViewLifecycleOwner(), pet -> {
            this.myPet = pet;
        });

        viewModel.getPotentialMatches().observe(getViewLifecycleOwner(), matches -> {
            this.potentialMatches = matches;
            currentMatchIndex = 0;
            showNextMatch();
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                if (emptyStateText != null) {
                    emptyStateText.setText(error);
                    emptyStateText.setVisibility(View.VISIBLE);
                }
                if (petCard != null) petCard.setVisibility(View.GONE);
            }
        });
    }

    private void showNextMatch() {
        if (!isAdded()) return;

        if (potentialMatches.isEmpty()) {
            if (emptyStateText != null) {
                emptyStateText.setText("No potential matches found.");
                emptyStateText.setVisibility(View.VISIBLE);
            }
            if (petCard != null) petCard.setVisibility(View.GONE);
            return;
        }

        if (petCard != null) petCard.setVisibility(View.VISIBLE);
        if (emptyStateText != null) emptyStateText.setVisibility(View.GONE);
        
        if (currentMatchIndex >= potentialMatches.size()) {
            currentMatchIndex = 0;
            Toast.makeText(getContext(), "Showing pets again...", Toast.LENGTH_SHORT).show();
        }

        PetProfile match = potentialMatches.get(currentMatchIndex);
        int score = myPet != null ? myPet.calculateMatchScore(match) : 50;

        if (petNameText != null) petNameText.setText(String.format(Locale.getDefault(), "%s, %d", match.getName(), match.getAge()));
        if (petBreedTagText != null) petBreedTagText.setText(String.format(Locale.getDefault(), "%s • %s", match.getBreed(), match.getSize()));
        if (petMatchScoreText != null) petMatchScoreText.setText(String.format(Locale.getDefault(), "%d%% Match", score));
        
        StringBuilder details = new StringBuilder();
        details.append("Weight: ").append(match.getWeight()).append("kg\n");
        details.append("Vaccinated: ").append(match.isVaccinated() ? "Yes" : "No").append("\n\n");
        
        if (match.getVibeTags() != null) {
            for (String tag : match.getVibeTags()) {
                details.append("#").append(tag).append(" ");
            }
        }
        if (petDescriptionText != null) petDescriptionText.setText(details.toString());

        if (petImageView != null) {
            Glide.with(this)
                    .load(match.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.icon_dog)
                    .into(petImageView);
        }

        if (matchButton != null) {
            matchButton.setOnClickListener(v -> showPlaydateRequestModal(match));
        }
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
                        currentMatchIndex++;
                        showNextMatch();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
