package com.petunity.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.petunity.R;
import com.petunity.activities.AddPetActivity;
import com.petunity.models.PetProfile;

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

        // UI Bindings
        petImageView = view.findViewById(R.id.petImage);
        petNameText = view.findViewById(R.id.dogName);
        petMatchScoreText = view.findViewById(R.id.petMatchScore);
        petBreedTagText = view.findViewById(R.id.petBreedTag);
        petDescriptionText = view.findViewById(R.id.petDescription);
        
        matchButton = view.findViewById(R.id.matchButton);
        passButton = view.findViewById(R.id.passButton);
        requestsButton = view.findViewById(R.id.requestsButton);
        addPetButton = view.findViewById(R.id.addPetButton);

        if (requestsButton != null) {
            requestsButton.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, new PlaydateRequestsFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (addPetButton != null) {
            addPetButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AddPetActivity.class);
                startActivity(intent);
            });
        }

        loadMyPetAndMatches();
    }

    private void loadMyPetAndMatches() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("pet_profiles")
                .whereEqualTo("ownerId", mAuth.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        myPet = queryDocumentSnapshots.getDocuments().get(0).toObject(PetProfile.class);
                        fetchOtherPets();
                    } else {
                        Toast.makeText(getContext(), "Please create a Pet Persona first!", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchOtherPets() {
        db.collection("pet_profiles")
                .whereNotEqualTo("ownerId", mAuth.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    potentialMatches.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        PetProfile other = doc.toObject(PetProfile.class);
                        other.setId(doc.getId());
                        potentialMatches.add(other);
                    }
                    showNextMatch();
                });
    }

    private void showNextMatch() {
        if (!isAdded()) return;

        if (potentialMatches.isEmpty()) {
            Toast.makeText(getContext(), "No other pets found yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentMatchIndex >= potentialMatches.size()) {
            currentMatchIndex = 0; // Loop back
            Toast.makeText(getContext(), "Showing pets again...", Toast.LENGTH_SHORT).show();
        }

        PetProfile match = potentialMatches.get(currentMatchIndex);
        int score = myPet != null ? myPet.calculateMatchScore(match) : 50;

        petNameText.setText(String.format(Locale.getDefault(), "%s, %d", match.getName(), match.getAge()));
        petBreedTagText.setText(String.format(Locale.getDefault(), "%s • %s", match.getBreed(), match.getSize()));
        petMatchScoreText.setText(String.format(Locale.getDefault(), "%d%% Match", score));
        
        StringBuilder details = new StringBuilder();
        details.append("Weight: ").append(match.getWeight()).append("kg\n");
        details.append("Vaccinated: ").append(match.isVaccinated() ? "Yes" : "No").append("\n\n");
        
        for (String tag : match.getVibeTags()) {
            details.append("#").append(tag).append(" ");
        }
        petDescriptionText.setText(details.toString());

        Glide.with(this)
                .load(match.getImageUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.icon_dog)
                .into(petImageView);

        matchButton.setOnClickListener(v -> showPlaydateRequestModal(match));
        passButton.setOnClickListener(v -> {
            currentMatchIndex++;
            showNextMatch();
        });
    }

    private void showPlaydateRequestModal(PetProfile match) {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_playdate_request, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        title.setText("Request Playdate with " + match.getName());

        TextInputEditText timeInput = dialogView.findViewById(R.id.timeInput);
        TextInputEditText locationInput = dialogView.findViewById(R.id.locationInput);
        MaterialButton sendButton = dialogView.findViewById(R.id.sendRequestButton);

        sendButton.setOnClickListener(v -> {
            String time = timeInput.getText().toString();
            String loc = locationInput.getText().toString();
            
            if (time.isEmpty() || loc.isEmpty()) {
                Toast.makeText(getContext(), "Please suggest a time and place", Toast.LENGTH_SHORT).show();
                return;
            }

            sendRequestToFirebase(match, time, loc);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void sendRequestToFirebase(PetProfile match, String time, String loc) {
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
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Playdate Request Sent!", Toast.LENGTH_SHORT).show();
                    currentMatchIndex++;
                    showNextMatch();
                });
    }
}
