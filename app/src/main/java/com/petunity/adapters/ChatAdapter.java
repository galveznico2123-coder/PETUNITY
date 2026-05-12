package com.petunity.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.petunity.R;
import com.petunity.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messages;
    private final String otherUserAvatarUrl;

    public ChatAdapter(List<Message> messages, String otherUserAvatarUrl) {
        this.messages = messages;
        this.otherUserAvatarUrl = otherUserAvatarUrl;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).isSentByMe()) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message, otherUserAvatarUrl);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        void bind(Message message) {
            messageText.setText(message.getText());
            timeText.setText(formatTime(message.getTimestampAsLong()));
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView senderAvatar;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            senderAvatar = itemView.findViewById(R.id.senderAvatar);
        }

        void bind(Message message, String avatarUrl) {
            messageText.setText(message.getText());
            timeText.setText(formatTime(message.getTimestampAsLong()));
            
            Glide.with(itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .into(senderAvatar);
        }
    }

    private static String formatTime(long timestamp) {
        try {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }
}
