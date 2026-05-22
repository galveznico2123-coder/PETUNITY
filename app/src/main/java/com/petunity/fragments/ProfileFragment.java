package com.petunity.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.petunity.activities.MembershipActivity;
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
    private UserManager userManager;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // UI will update automatically via LiveData observers
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
        userManager = UserManager.getInstance();

        initViews(view);
        setupListeners(view);
        setupObservers();
        
        // Refresh data from Firestore in background
        refreshUserDataFromFirestore();
        fetchReportCount();
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        profileName = view.findViewById(R.id.profileName);
        membershipType = view.findViewById(R.id.membershipType);
        profileLocation = view.findViewById(R.id.profileLocation);
        profileBio = view.findViewById(R.id.profileBio);
        txtReportCount = view.findViewById(R.id.txtReportCount);
        txtPetsHelped = view.findViewById(R.id.txtPetsHelped);
        txtPointsLabel = view.findViewById(R.id.txtPointsLabel);
    }

    private void setupListeners(View view) {
        MaterialButton btnEditProfile = view.findViewById(R.id.btnEditProfile);
        MaterialButton logoutButton = view.findViewById(R.id.logoutButton);
        MaterialButton myReportsButton = view.findViewById(R.id.btnMyReports);
        MaterialButton myMembershipButton = view.findViewById(R.id.btnMyMembership);
        View reportsClickArea = view.findViewById(R.id.reportsClickArea);
        MaterialButton btnMyPets = view.findViewById(R.id.btnMyPets);

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                editProfileLauncher.launch(intent);
            });
        }

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }

        if (myReportsButton != null) {
            myReportsButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyPostsActivity.class)));
        }

        if (myMembershipButton != null) {
            myMembershipButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), MembershipActivity.class)));
        }

        if (reportsClickArea != null) {
            reportsClickArea.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyPostsActivity.class)));
        }
        
        if (btnMyPets != null) {
            btnMyPets.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyPetsActivity.class)));
        }
    }

    private void setupObservers() {
        // Observe profile image for real-time updates
        userManager.getProfileImageLiveData().observe(getViewLifecycleOwner(), url -> {
            if (isAdded() && profileImage != null) {
                Glide.with(this)
                        .load(url)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(profileImage);
            }
        });

        // Observe name for real-time updates
        userManager.getUserNameLiveData().observe(getViewLifecycleOwner(), name -> {
            if (isAdded() && profileName != null) {
                profileName.setText(name != null && !name.isEmpty() ? name : "User");
            }
        });

        // Other fields update on resume or after edit
        updateUIFields();
    }

    private void logout() {
        SharedPreferences prefs = requireContext().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(LoginActivity.PREF_AUTO_LOGIN, false).apply();
        
        mAuth.signOut();
        userManager.clear();
        
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void updateUIFields() {
        if (profileLocation != null) {
            String loc = userManager.getLocation();
            if (loc != null && !loc.isEmpty()) {
                profileLocation.setText(loc);
                profileLocation.setVisibility(View.VISIBLE);
            } else {
                profileLocation.setVisibility(View.GONE);
            }
        }

        if (profileBio != null) {
            String bio = userManager.getBio();
            if (bio != null && !bio.isEmpty()) {
                profileBio.setText(bio);
            } else {
                profileBio.setText(R.string.no_bio_added);
            }
        }

        if (txtPetsHelped != null) {
            txtPetsHelped.setText(String.format(Locale.getDefault(), "%.1f", userManager.getPetsHelped()));
        }

        String level = userManager.getMembershipLevelName();
        if (txtPointsLabel != null) txtPointsLabel.setText(level);
        if (membershipType != null) membershipType.setText(level);
    }

    private void refreshUserDataFromFirestore() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        userManager.setName(doc.getString("name"));
                        userManager.setLocation(doc.getString("location"));
                        userManager.setBio(doc.getString("bio"));
                        userManager.setProfileImageUrl(doc.getString("profileImageUrl"));
                        userManager.setEmail(doc.getString("email"));
                        userManager.setPhone(doc.getString("phone"));
                        
                        Double helped = doc.getDouble("petsHelped");
                        if (helped != null) userManager.setPetsHelped(helped);
                        
                        String mType = doc.getString("membershipType");
                        if (mType != null) userManager.setMembershipType(mType);

                        updateUIFields();
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
                    if (txtReportCount != null && isAdded()) {
                        txtReportCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                    }
                });
    }
}
