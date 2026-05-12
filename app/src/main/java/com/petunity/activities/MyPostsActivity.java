package com.petunity.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.petunity.R;
import com.petunity.adapters.PostsAdapter;
import com.petunity.models.Post;

import java.util.ArrayList;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity {
    private static final String TAG = "MyPostsActivity";
    private PostsAdapter adapter;
    private List<Post> myPostsList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        RecyclerView recyclerView = findViewById(R.id.myPostsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PostsAdapter(myPostsList);
        recyclerView.setAdapter(adapter);

        fetchMyPosts();
    }

    private void fetchMyPosts() {
        String currentUserId = mAuth.getUid();
        if (currentUserId == null) {
            if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("posts")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        myPostsList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setId(doc.getId());
                                myPostsList.add(post);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (emptyText != null) {
                            emptyText.setVisibility(myPostsList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }
}