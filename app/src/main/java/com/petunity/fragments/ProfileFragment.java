package com.petunity.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.activities.EditProfileActivity;
import com.petunity.activities.LoginActivity;
import com.petunity.activities.MyPetsActivity;
import com.petunity.activities.MyPostsActivity;
import com.petunity.models.UserManager;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ImageView profileImage;
    private TextView profileName, membershipType, txtReportCount, profileLocation, profileBio;
    private TextView txtPetsHelped, txtPointsLabel;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadUserData();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        profileImage = view.findViewById(R.id.profileImage);
        profileName = view.findViewById(R.id.profileName);
        membershipType = view.findViewById(R.id.membershipType);
        profileLocation = view.findViewById(R.id.profileLocation);
        profileBio = view.findViewById(R.id.profileBio);
        txtReportCount = view.findViewById(R.id.txtReportCount);
        
        // Stats mapping for Membership Level
        txtPetsHelped = view.findViewById(R.id.txtPetsHelped);
        txtPointsLabel = view.findViewById(R.id.txtPointsLabel);
        
        MaterialButton btnEditProfile = view.findViewById(R.id.btnEditProfile);
        MaterialButton logoutButton = view.findViewById(R.id.logoutButton);
        MaterialButton myReportsButton = view.findViewById(R.id.btnMyReports);
        View reportsClickArea = view.findViewById(R.id.reportsClickArea);
        MaterialButton btnMyPets = view.findViewById(R.id.btnMyPets);

        loadUserData();
        fetchReportCount();
        updateMembershipStats();

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                editProfileLauncher.launch(intent);
            });
        }

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                mAuth.signOut();
                startActivity(new Intent(requireContext(), LoginActivity.class));
                requireActivity().finish();
            });
        }

        if (myReportsButton != null) {
            myReportsButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyPostsActivity.class)));
        }

        if (reportsClickArea != null) {
            reportsClickArea.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyPostsActivity.class)));
        }
        
        if (btnMyPets != null) {
            btnMyPets.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyPetsActivity.class)));
        }
    }

    private void updateMembershipStats() {
        UserManager user = UserManager.getInstance();
        if (txtPetsHelped != null) {
            txtPetsHelped.setText(String.format(Locale.getDefault(), "%.1f", user.getPetsHelped()));
        }
        if (txtPointsLabel != null) {
            txtPointsLabel.setText(user.getMembershipLevelName());
        }
        if (membershipType != null) {
            membershipType.setText(user.getMembershipLevelName());
        }
    }

    private void loadUserData() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String location = doc.getString("location");
                        String bio = doc.getString("bio");
                        String url = doc.getString("profileImageUrl");
                        Double helped = doc.getDouble("petsHelped");

                        if (helped != null) {
                            UserManager.getInstance().setPetsHelped(helped);
                            updateMembershipStats();
                        }

                        if (profileName != null && name != null) profileName.setText(name);
                        
                        if (profileLocation != null) {
                            if (location != null && !location.isEmpty()) {
                                profileLocation.setText(location);
                                profileLocation.setVisibility(View.VISIBLE);
                            } else {
                                profileLocation.setVisibility(View.GONE);
                            }
                        }
                        if (profileBio != null) {
                            if (bio != null && !bio.isEmpty()) {
                                profileBio.setText(bio);
                            } else {
                                profileBio.setText(R.string.no_bio_added);
                            }
                        }
                        
                        if (profileImage != null && isAdded()) {
                            Glide.with(this)
                                    .load(url)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_user)
                                    .into(profileImage);
                        }
                    }
                });
    }

    private void fetchReportCount() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("alerts")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (txtReportCount != null) {
                        txtReportCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                    }
                });
    }
}
