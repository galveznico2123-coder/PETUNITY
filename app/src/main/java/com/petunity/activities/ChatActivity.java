package com.petunity.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.petunity.R;
import com.petunity.databinding.ActivityChatBinding;
import com.petunity.models.Message;
import com.petunity.adapters.ChatAdapter;
import com.petunity.models.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private ActivityChatBinding binding;
    private ChatAdapter adapter;
    private List<Message> messages;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserAvatarUrl;
    private String conversationId;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getUid();

        otherUserId = getIntent().getStringExtra("other_user_id");
        otherUserName = getIntent().getStringExtra("user_name");
        otherUserAvatarUrl = getIntent().getStringExtra("avatar_url");

        if (currentUserId == null || otherUserId == null) {
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.chatName.setText(otherUserName != null ? otherUserName : "Chat");
        Glide.with(this)
                .load(otherUserAvatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_user)
                .into(binding.chatAvatar);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        conversationId = currentUserId.compareTo(otherUserId) < 0 
                ? currentUserId + "_" + otherUserId 
                : otherUserId + "_" + currentUserId;

        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages, otherUserAvatarUrl);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); 
        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setAdapter(adapter);

        listenForMessages();

        binding.sendButton.setOnClickListener(v -> sendMessage());
    }

    private void listenForMessages() {
        if (messageListener != null) messageListener.remove();
        
        messageListener = db.collection("chat_collection")
                .whereEqualTo("conversationId", conversationId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed!", error);
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                message.setSentByMe(message.getSenderId().equals(currentUserId));
                                
                                boolean exists = false;
                                for (Message m : messages) {
                                    if (m.getTimestampAsLong() == message.getTimestampAsLong() && m.getText().equals(message.getText())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                
                                if (!exists) {
                                    messages.add(message);
                                    adapter.notifyItemInserted(messages.size() - 1);
                                    binding.chatRecyclerView.scrollToPosition(messages.size() - 1);
                                }
                            }
                        }
                    }
                });
    }

    private void sendMessage() {
        String text = binding.messageInput.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            long timestamp = System.currentTimeMillis();
            Message newMessage = new Message(currentUserId, otherUserId, text, timestamp);
            newMessage.setSentByMe(true);
            
            messages.add(newMessage);
            adapter.notifyItemInserted(messages.size() - 1);
            binding.chatRecyclerView.scrollToPosition(messages.size() - 1);
            binding.messageInput.setText(""); 

            db.collection("chat_collection").add(newMessage)
                    .addOnSuccessListener(documentReference -> {
                        updateConversationSummary(text, timestamp);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error sending message", e);
                        Toast.makeText(this, "Send Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void updateConversationSummary(String lastMsg, long timestamp) {
        Map<String, Object> conversation = new HashMap<>();
        conversation.put("lastMessage", lastMsg);
        conversation.put("lastTimestamp", timestamp);
        conversation.put("participants", Arrays.asList(currentUserId, otherUserId));
        
        String currentUserName = UserManager.getInstance().getName();
        Map<String, String> namesMap = new HashMap<>();
        namesMap.put(currentUserId, currentUserName);
        namesMap.put(otherUserId, otherUserName);
        conversation.put("names", namesMap);
        
        conversation.put("lastSenderId", currentUserId);
        conversation.put("lastSenderName", currentUserName);

        db.collection("conversations").document(conversationId)
                .set(conversation, SetOptions.merge());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}
