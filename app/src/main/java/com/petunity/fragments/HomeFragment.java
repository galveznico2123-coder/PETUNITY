package com.petunity.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.activities.MembershipActivity;
import com.petunity.adapters.PostsAdapter;
import com.petunity.models.Post;
import com.petunity.models.UserManager;
import com.petunity.viewmodels.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private PostsAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserProfileImageUrl;
    
    private TextView helpedCountText, activeCountText, nearYouCountText, welcomeUserText, userRankText, pointsToNextLevel;
    private ImageView homeProfileImage;
    private ProgressBar loadingProgressBar;
    private LinearProgressIndicator miniMembershipProgress;
    private TextView emptyStateText;
    private HomeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        welcomeUserText = view.findViewById(R.id.welcomeUserText);
        userRankText = view.findViewById(R.id.userRankText);
        homeProfileImage = view.findViewById(R.id.homeProfileImage);
        helpedCountText = view.findViewById(R.id.helpedCountText);
        activeCountText = view.findViewById(R.id.activeCountText);
        nearYouCountText = view.findViewById(R.id.nearYouCountText);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        miniMembershipProgress = view.findViewById(R.id.miniMembershipProgress);
        pointsToNextLevel = view.findViewById(R.id.pointsToNextLevel);
        
        View membershipCard = view.findViewById(R.id.membershipMiniCard);
        if (membershipCard != null) {
            membershipCard.setOnClickListener(v -> startActivity(new Intent(requireContext(), MembershipActivity.class)));
        }

        RecyclerView recyclerView = view.findViewById(R.id.postsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PostsAdapter(postList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton addPostFab = view.findViewById(R.id.addPostFab);
        if (addPostFab != null) {
            addPostFab.setOnClickListener(v -> showCreatePostDialog());
        }

        updateMembershipUI();
        observeViewModel();
        fetchCurrentUserProfileImage();
        fetchStats();

        String currentUid = mAuth.getUid();
        if (currentUid != null) {
            viewModel.fetchPosts(currentUid);
        }
    }

    private void updateMembershipUI() {
        UserManager user = UserManager.getInstance();
        if (welcomeUserText != null) {
            String name = user.getName();
            welcomeUserText.setText(name != null ? "Hi, " + name + "!" : "Welcome Hero!");
        }
        if (userRankText != null) {
            userRankText.setText(user.getMembershipLevelName());
        }
        
        double helped = user.getPetsHelped();
        int currentPoints = (int) Math.ceil(helped);
        int nextThreshold = 6;
        String nextEmoji = "🐶";
        
        if (currentPoints >= 6 && currentPoints < 16) { nextThreshold = 16; nextEmoji = "🛡️"; }
        else if (currentPoints >= 16 && currentPoints < 31) { nextThreshold = 31; nextEmoji = "🏆"; }
        else if (currentPoints >= 31 && currentPoints < 51) { nextThreshold = 51; nextEmoji = "🦸"; }
        else if (currentPoints >= 51) { nextThreshold = 100; nextEmoji = "🔥"; }

        if (miniMembershipProgress != null) {
            int percent = (int) ((helped / nextThreshold) * 100);
            miniMembershipProgress.setProgress(percent);
        }
        if (pointsToNextLevel != null) {
            int needed = nextThreshold - currentPoints;
            pointsToNextLevel.setText(needed > 0 ? needed + " pts to " + nextEmoji : "Max Rank!");
        }
    }

    private void observeViewModel() {
        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            postList.clear();
            postList.addAll(posts);
            adapter.notifyDataSetChanged();
            
            if (emptyStateText != null) {
                emptyStateText.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCurrentUserProfileImage() {
        String uid = mAuth.getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (!isAdded()) return;
                currentUserProfileImageUrl = documentSnapshot.getString("profileImageUrl");
                if (currentUserProfileImageUrl != null && !currentUserProfileImageUrl.isEmpty()) {
                    Glide.with(this).load(currentUserProfileImageUrl).circleCrop().placeholder(R.drawable.ic_user).into(homeProfileImage);
                } else if (homeProfileImage != null) {
                    homeProfileImage.setImageResource(R.drawable.ic_user);
                }
            });
        }
    }

    private void fetchStats() {
        db.collection("reunions").addSnapshotListener((value, error) -> {
            if (!isAdded()) return;
            if (value != null && helpedCountText != null) {
                helpedCountText.setText(String.valueOf(value.size()));
            }
        });

        db.collection("posts").addSnapshotListener((value, error) -> {
            if (!isAdded()) return;
            if (value != null && activeCountText != null) {
                activeCountText.setText(String.valueOf(value.size()));
            }
        });

        db.collection("pet_listing").addSnapshotListener((value, error) -> {
            if (!isAdded()) return;
            if (value != null && nearYouCountText != null) {
                nearYouCountText.setText(String.valueOf(value.size()));
            }
        });
    }

    private void showCreatePostDialog() {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null);
        EditText titleInput = dialogView.findViewById(R.id.postTitleInput);
        EditText contentInput = dialogView.findViewById(R.id.postContentInput);
        CheckBox urgentCheckBox = dialogView.findViewById(R.id.urgentCheckBox);

        new AlertDialog.Builder(requireContext())
                .setTitle("New Report")
                .setView(dialogView)
                .setPositiveButton("Post", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String content = contentInput.getText().toString().trim();
                    boolean isUrgent = urgentCheckBox.isChecked();
                    
                    if (!title.isEmpty() && !content.isEmpty()) {
                        savePostToFirebase(title, content, isUrgent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void savePostToFirebase(String title, String content, boolean isUrgent) {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        String userName = UserManager.getInstance().getName();

        Post newPost = new Post(userName, "Just now", title, content);
        newPost.setUserId(userId);
        newPost.setUserProfileImageUrl(currentUserProfileImageUrl);
        newPost.setUrgent(isUrgent);
        newPost.setPrivate(false);

        db.collection("posts").add(newPost)
                .addOnSuccessListener(documentReference -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Posted successfully!", Toast.LENGTH_SHORT).show();
                        updateMembershipUI(); // Refresh UI after posting
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to post", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
