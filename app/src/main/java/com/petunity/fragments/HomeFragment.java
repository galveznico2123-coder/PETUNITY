package com.petunity.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.petunity.R;
import com.petunity.adapters.PostsAdapter;
import com.petunity.models.Post;
import com.petunity.models.UserManager;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private PostsAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserProfileImageUrl;
    
    private TextView helpedCountText, activeCountText, nearYouCountText, welcomeUserText;
    private ImageView homeProfileImage;

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
        
        welcomeUserText = view.findViewById(R.id.welcomeUserText);
        homeProfileImage = view.findViewById(R.id.homeProfileImage);
        helpedCountText = view.findViewById(R.id.helpedCountText);
        activeCountText = view.findViewById(R.id.activeCountText);
        nearYouCountText = view.findViewById(R.id.nearYouCountText);
        
        if (welcomeUserText != null) {
            String name = UserManager.getInstance().getName();
            welcomeUserText.setText(name != null ? name + "!" : "Hero!");
        }

        RecyclerView recyclerView = view.findViewById(R.id.postsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PostsAdapter(postList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton addPostFab = view.findViewById(R.id.addPostFab);
        addPostFab.setOnClickListener(v -> showCreatePostDialog());

        fetchCurrentUserProfileImage();
        fetchPosts();
        fetchStats();
    }

    private void fetchCurrentUserProfileImage() {
        String uid = mAuth.getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                currentUserProfileImageUrl = documentSnapshot.getString("profileImageUrl");
                if (currentUserProfileImageUrl != null && !currentUserProfileImageUrl.isEmpty() && isAdded()) {
                    Glide.with(this).load(currentUserProfileImageUrl).circleCrop().placeholder(R.drawable.ic_user).into(homeProfileImage);
                } else if (homeProfileImage != null) {
                    homeProfileImage.setImageResource(R.drawable.ic_user);
                }
            });
        }
    }

    private void fetchStats() {
        db.collection("reunions").addSnapshotListener((value, error) -> {
            if (value != null && helpedCountText != null) {
                helpedCountText.setText(String.valueOf(value.size()));
            }
        });

        db.collection("posts").addSnapshotListener((value, error) -> {
            if (value != null && activeCountText != null) {
                activeCountText.setText(String.valueOf(value.size()));
            }
        });

        db.collection("pet_listing").addSnapshotListener((value, error) -> {
            if (value != null && nearYouCountText != null) {
                nearYouCountText.setText(String.valueOf(value.size()));
            }
        });
    }

    private void showCreatePostDialog() {
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
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        String userName = UserManager.getInstance().getName();

        Post newPost = new Post(userName, "Just now", title, content);
        newPost.setUserId(userId);
        newPost.setUserProfileImageUrl(currentUserProfileImageUrl); // Save profile image URL with post
        newPost.setUrgent(isUrgent);
        newPost.setPrivate(false); // Community posts are public by default

        db.collection("posts").add(newPost)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Posted successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to post", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchPosts() {
        String currentUid = mAuth.getUid();
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        postList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setId(doc.getId());
                                
                                // Privacy Logic: 
                                // 1. Show if post is NOT private
                                // 2. OR show if I am one of the participants in this private post
                                if (!post.isPrivate() || 
                                   (post.getParticipants() != null && post.getParticipants().contains(currentUid))) {
                                    postList.add(post);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
