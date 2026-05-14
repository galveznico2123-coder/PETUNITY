package com.petunity.adapters;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.activities.ChatActivity;
import com.petunity.databinding.ItemPostBinding;
import com.petunity.models.Post;
import com.petunity.models.UserManager;
import com.petunity.utils.TimeUtils;

import java.util.List;
import java.util.Locale;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {
    private final List<Post> posts;

    public PostsAdapter(List<Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostBinding binding;

        public PostViewHolder(@NonNull ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Post post) {
            String currentUserId = FirebaseAuth.getInstance().getUid();
            
            binding.userNameText.setText(post.getUserName());
            
            // Load user avatar with fallback
            if (post.getUserProfileImageUrl() != null && !post.getUserProfileImageUrl().isEmpty()) {
                Glide.with(binding.userAvatar.getContext())
                        .load(post.getUserProfileImageUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(binding.userAvatar);
            } else if (post.getUserId() != null) {
                // Fallback: Fetch from users collection if missing in post
                FirebaseFirestore.getInstance().collection("users").document(post.getUserId()).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String url = doc.getString("profileImageUrl");
                                post.setUserProfileImageUrl(url);
                                Glide.with(binding.userAvatar.getContext())
                                        .load(url)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_user)
                                        .into(binding.userAvatar);
                            } else {
                                binding.userAvatar.setImageResource(R.drawable.ic_user);
                            }
                        });
            } else {
                binding.userAvatar.setImageResource(R.drawable.ic_user);
            }

            // Display relative time
            if (post.getTimestamp() != null) {
                binding.timeText.setText(TimeUtils.getTimeAgo(post.getTimestamp().toDate()));
            } else {
                binding.timeText.setText(post.getTimeLabel());
            }

            binding.postContentText.setText(post.getContent());

            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                binding.postImageCard.setVisibility(View.VISIBLE);
                Glide.with(binding.postImageView.getContext())
                        .load(post.getImageUrl())
                        .placeholder(R.drawable.logo_pet)
                        .into(binding.postImageView);
            } else {
                binding.postImageCard.setVisibility(View.GONE);
            }

            // Status & Visibility Logic
            boolean isOwner = post.getUserId() != null && post.getUserId().equals(currentUserId);
            
            if (post.isSaved()) {
                binding.savedBadge.setVisibility(View.VISIBLE);
                binding.helpButton.setVisibility(View.GONE);
                binding.confirmSavedButton.setVisibility(View.GONE);
            } else {
                binding.savedBadge.setVisibility(View.GONE);
                if (isOwner) {
                    binding.confirmSavedButton.setVisibility(View.VISIBLE);
                    binding.helpButton.setVisibility(View.GONE);
                } else {
                    binding.confirmSavedButton.setVisibility(View.GONE);
                    binding.helpButton.setVisibility(View.VISIBLE);
                }
            }

            if (isOwner) {
                binding.deleteButton.setVisibility(View.VISIBLE);
                binding.deleteButton.setOnClickListener(v -> deletePost(post));
            } else {
                binding.deleteButton.setVisibility(View.GONE);
            }

            binding.shareButton.setOnClickListener(v -> showShareDialog(post));
            
            View.OnClickListener chatOpener = v -> {
                if (post.getUserId() == null) {
                    Toast.makeText(binding.getRoot().getContext(), "Owner info unavailable", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (post.getUserId().equals(currentUserId)) {
                    Toast.makeText(binding.getRoot().getContext(), "You cannot message yourself", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Intent intent = new Intent(binding.getRoot().getContext(), ChatActivity.class);
                intent.putExtra("other_user_id", post.getUserId());
                intent.putExtra("user_name", post.getUserName());
                intent.putExtra("avatar_url", post.getUserProfileImageUrl());
                binding.getRoot().getContext().startActivity(intent);
            };

            binding.helpButton.setOnClickListener(chatOpener);
            binding.messageButton.setOnClickListener(chatOpener);

            binding.confirmSavedButton.setOnClickListener(v -> confirmPetSaved(post));
            
            binding.detailsButton.setOnClickListener(v -> {
                Toast.makeText(binding.getRoot().getContext(), "Reporting detailed info...", Toast.LENGTH_SHORT).show();
            });
        }

        private void confirmPetSaved(Post post) {
            new AlertDialog.Builder(binding.getRoot().getContext())
                    .setTitle("Confirm Pet Saved")
                    .setMessage("Is the pet safely back home or in good condition?")
                    .setPositiveButton("Yes, Saved!", (dialog, which) -> {
                        FirebaseFirestore.getInstance().collection("posts")
                                .document(post.getId())
                                .update("saved", true)
                                .addOnSuccessListener(aVoid -> {
                                    post.setSaved(true);
                                    notifyItemChanged(getAdapterPosition());
                                    Toast.makeText(binding.getRoot().getContext(), "Great news! Post updated.", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void deletePost(Post post) {
            new AlertDialog.Builder(binding.getRoot().getContext())
                    .setTitle("Delete Post")
                    .setMessage("This will permanently remove the alert. Continue?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        FirebaseFirestore.getInstance().collection("posts")
                                .document(post.getId()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(binding.getRoot().getContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void showShareDialog(Post originalPost) {
            EditText input = new EditText(binding.getRoot().getContext());
            input.setHint("Add an optional message...");
            new AlertDialog.Builder(binding.getRoot().getContext())
                    .setTitle("Share Alert")
                    .setView(input)
                    .setPositiveButton("Share", (dialog, which) -> {
                        sharePost(originalPost, input.getText().toString().trim());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void sharePost(Post originalPost, String userDescription) {
            String currentUserId = FirebaseAuth.getInstance().getUid();
            String sharedContent = (userDescription.isEmpty() ? "" : userDescription + "\n\n") 
                                 + "--- Shared Alert ---\n" + originalPost.getContent();
            
            Post sharedPost = new Post(UserManager.getInstance().getName(), "Just now", "Shared: " + originalPost.getTitle(), sharedContent);
            sharedPost.setUserId(currentUserId);
            sharedPost.setImageUrl(originalPost.getImageUrl());
            sharedPost.setUrgent(originalPost.isUrgent());
            sharedPost.setSourcePostId(originalPost.getId());

            FirebaseFirestore.getInstance().collection("posts").add(sharedPost)
                    .addOnSuccessListener(doc -> Toast.makeText(binding.getRoot().getContext(), "Shared to feed!", Toast.LENGTH_SHORT).show());
        }
    }
}
