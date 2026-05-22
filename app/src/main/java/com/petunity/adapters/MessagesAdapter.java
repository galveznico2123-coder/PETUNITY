package com.petunity.adapters;

import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.petunity.R;
import com.petunity.activities.ChatActivity;
import com.petunity.databinding.ItemConversationBinding;
import com.petunity.models.Conversation;

import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ConversationViewHolder> {
    private final List<Conversation> conversations;

    public MessagesAdapter(List<Conversation> conversations) {
        this.conversations = conversations;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConversationBinding binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ConversationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(conversations.get(position));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void updateConversation(Conversation conversation) {
        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).getOtherUserId().equals(conversation.getOtherUserId())) {
                conversations.set(i, conversation);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding binding;

        public ConversationViewHolder(@NonNull ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Conversation conversation) {
            String currentUserId = FirebaseAuth.getInstance().getUid();
            boolean unread = conversation.isUnread(currentUserId);

            String displayName = conversation.getName() != null ? conversation.getName() : "Loading...";
            binding.userNameText.setText(displayName);
            binding.lastMessageText.setText(conversation.getLastMessage());
            binding.timeText.setText(conversation.getTime());

            if (unread) {
                binding.lastMessageText.setTypeface(null, Typeface.BOLD);
                binding.lastMessageText.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_primary));
                binding.userNameText.setTypeface(null, Typeface.BOLD);
            } else {
                binding.lastMessageText.setTypeface(null, Typeface.NORMAL);
                binding.lastMessageText.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_secondary));
                binding.userNameText.setTypeface(null, Typeface.NORMAL);
            }

            Glide.with(binding.getRoot().getContext())
                    .load(conversation.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .into(binding.avatarImage);

            binding.onlineIndicator.setVisibility(conversation.isOnline() ? android.view.View.VISIBLE : android.view.View.GONE);

            binding.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                intent.putExtra("other_user_id", conversation.getOtherUserId());
                intent.putExtra("user_name", conversation.getName());
                intent.putExtra("avatar_url", conversation.getAvatarUrl());
                v.getContext().startActivity(intent);
            });
        }
    }
}
