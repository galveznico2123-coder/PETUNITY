package com.petunity.activities;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding binding;

        public ConversationViewHolder(@NonNull ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Conversation conversation) {
            binding.userNameText.setText(conversation.getName());
            binding.lastMessageText.setText(conversation.getLastMessage());
            binding.timeText.setText(conversation.getTime());

            Glide.with(binding.getRoot().getContext())
                    .load(conversation.getAvatarUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .into(binding.avatarImage);
        }
    }
}