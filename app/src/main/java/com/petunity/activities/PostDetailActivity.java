package com.petunity.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.models.Post;
import com.petunity.utils.TimeUtils;

public class PostDetailActivity extends AppCompatActivity {

    private TextView postTitle, userName, postTime, postDescription;
    private ImageView userAvatar, postImage;
    private View urgentBadge, imageCard;
    private MaterialButton btnContact;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        postId = getIntent().getStringExtra("post_id");
        Post post = (Post) getIntent().getSerializableExtra("post_object");

        initViews();

        if (post != null) {
            displayPost(post);
        } else if (postId != null) {
            fetchPostFromFirestore(postId);
        } else {
            Toast.makeText(this, "Error: Post not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        postTitle = findViewById(R.id.postTitle);
        userName = findViewById(R.id.userName);
        postTime = findViewById(R.id.postTime);
        postDescription = findViewById(R.id.postDescription);
        userAvatar = findViewById(R.id.userAvatar);
        postImage = findViewById(R.id.postImage);
        urgentBadge = findViewById(R.id.urgentBadge);
        imageCard = findViewById(R.id.imageCard);
        btnContact = findViewById(R.id.btnContact);
    }

    private void fetchPostFromFirestore(String id) {
        FirebaseFirestore.getInstance().collection("posts").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Post post = documentSnapshot.toObject(Post.class);
                    if (post != null) {
                        post.setId(documentSnapshot.getId());
                        displayPost(post);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load post", Toast.LENGTH_SHORT).show());
    }

    private void displayPost(Post post) {
        postTitle.setText(post.getTitle());
        userName.setText(post.getUserName());
        postDescription.setText(post.getContent());
        
        if (post.getTimestamp() != null) {
            postTime.setText(TimeUtils.getTimeAgo(post.getTimestamp().toDate()));
        }

        urgentBadge.setVisibility(post.isUrgent() ? View.VISIBLE : View.GONE);

        if (post.getUserProfileImageUrl() != null && !post.getUserProfileImageUrl().isEmpty()) {
            Glide.with(this).load(post.getUserProfileImageUrl()).circleCrop().into(userAvatar);
        }

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            imageCard.setVisibility(View.VISIBLE);
            Glide.with(this).load(post.getImageUrl()).into(postImage);
        } else {
            imageCard.setVisibility(View.GONE);
        }

        btnContact.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("other_user_id", post.getUserId());
            intent.putExtra("user_name", post.getUserName());
            intent.putExtra("avatar_url", post.getUserProfileImageUrl());
            startActivity(intent);
        });
    }
}
