package com.petunity.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private PostsAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    
    private TextView helpedCountText, activeCountText, nearYouCountText, welcomeUserText, userRankText, pointsToNextLevel;
    private ImageView homeProfileImage;
    private ShimmerFrameLayout shimmerViewContainer;
    private LinearProgressIndicator miniMembershipProgress;
    private TextView emptyStateText;
    private HomeViewModel viewModel;
    private RecyclerView recyclerView;
    private UserManager userManager;

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
        userManager = UserManager.getInstance();
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        initViews(view);
        setupClickListeners(view);
        setupObservers();
        fetchStats();

        String currentUid = mAuth.getUid();
        if (currentUid != null) {
            viewModel.fetchPosts(currentUid);
        }
    }

    private void initViews(View view) {
        welcomeUserText = view.findViewById(R.id.welcomeUserText);
        userRankText = view.findViewById(R.id.userRankText);
        homeProfileImage = view.findViewById(R.id.homeProfileImage);
        helpedCountText = view.findViewById(R.id.helpedCountText);
        activeCountText = view.findViewById(R.id.activeCountText);
        nearYouCountText = view.findViewById(R.id.nearYouCountText);
        shimmerViewContainer = view.findViewById(R.id.shimmerViewContainer);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        miniMembershipProgress = view.findViewById(R.id.miniMembershipProgress);
        pointsToNextLevel = view.findViewById(R.id.pointsToNextLevel);
        recyclerView = view.findViewById(R.id.postsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PostsAdapter(postList);
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners(View view) {
        View membershipCard = view.findViewById(R.id.membershipMiniCard);
        if (membershipCard != null) {
            membershipCard.setOnClickListener(v -> startActivity(new Intent(requireContext(), MembershipActivity.class)));
        }

        FloatingActionButton addAlertFab = view.findViewById(R.id.addAlertFab);
        if (addAlertFab != null) {
            addAlertFab.setOnClickListener(v -> {
                if (getActivity() != null) {
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.navigation_alert);
                    }
                }
            });
        }
    }

    private void setupObservers() {
        // Observe Real-time Profile Updates from UserManager
        userManager.getProfileImageLiveData().observe(getViewLifecycleOwner(), url -> {
            if (isAdded() && homeProfileImage != null) {
                Glide.with(this)
                        .load(url)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(homeProfileImage);
            }
        });

        userManager.getNameLiveData().observe(getViewLifecycleOwner(), name -> {
            if (isAdded() && welcomeUserText != null) {
                welcomeUserText.setText(name != null && !name.isEmpty() ? "Hi, " + name + "!" : "Welcome Hero!");
            }
        });

        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            postList.clear();
            postList.addAll(posts);
            adapter.notifyDataSetChanged();
            if (emptyStateText != null) {
                emptyStateText.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            }
            if (shimmerViewContainer != null) {
                shimmerViewContainer.stopShimmer();
                shimmerViewContainer.setVisibility(View.GONE);
            }
            recyclerView.setVisibility(View.VISIBLE);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                recyclerView.setVisibility(View.GONE);
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.setVisibility(View.VISIBLE);
                    shimmerViewContainer.startShimmer();
                }
            }
        });

        updateMembershipUI();
    }

    private void updateMembershipUI() {
        if (userRankText != null) {
            userRankText.setText(userManager.getMembershipLevelName());
        }
        
        double helped = userManager.getPetsHelped();
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

    @Override
    public void onResume() {
        super.onResume();
        if (shimmerViewContainer != null && shimmerViewContainer.getVisibility() == View.VISIBLE) {
            shimmerViewContainer.startShimmer();
        }
    }

    @Override
    public void onPause() {
        if (shimmerViewContainer != null) shimmerViewContainer.stopShimmer();
        super.onPause();
    }
}
