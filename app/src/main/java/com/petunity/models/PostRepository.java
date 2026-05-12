package com.petunity.models;

import java.util.ArrayList;
import java.util.List;

public class PostRepository {
    private static PostRepository instance;
    private final List<Post> posts = new ArrayList<>();
    private OnPostAddedListener listener;

    public interface OnPostAddedListener {
        void onPostAdded();
    }

    private PostRepository() {
        // Initial dummy data
        posts.add(new Post("Alex Chen", "2 hours ago · Central Park", "Friendly Stray",
                "Spotted near Central Park! Very friendly and seems to be looking for help. Wearing a red collar."));
        posts.add(new Post("Maria Garcia", "5 hours ago · Brooklyn", "Lost Cat Alert",
                "My cat Luna has been missing since yesterday. Please contact if seen!"));
    }

    public static synchronized PostRepository getInstance() {
        if (instance == null) {
            instance = new PostRepository();
        }
        return instance;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void addPost(Post post) {
        posts.add(0, post);
        if (listener != null) {
            listener.onPostAdded();
        }
    }

    public void setOnPostAddedListener(OnPostAddedListener listener) {
        this.listener = listener;
    }
}