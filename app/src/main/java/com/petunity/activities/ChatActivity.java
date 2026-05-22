package com.petunity.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.petunity.utils.PetTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    public static String activeChatUserId = null;

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
    private ListenerRegistration presenceListener;

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

        markAsRead();
        listenForMessages();
        listenForPresence();

        binding.sendButton.setOnClickListener(v -> sendMessage());
    }

    private void markAsRead() {
        if (currentUserId == null || conversationId == null) return;

        Map<String, Object> readUpdate = new HashMap<>();
        readUpdate.put("readStatus." + currentUserId, true);

        db.collection("conversations").document(conversationId)
                .update(readUpdate)
                .addOnFailureListener(e -> {
                    // If conversation doesn't exist yet, we don't need to mark as read
                    Log.d(TAG, "Conversation doesn't exist yet for read update");
                });
    }

    private void listenForPresence() {
        if (otherUserId == null) return;
        
        presenceListener = db.collection("users").document(otherUserId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;

                    Boolean online = doc.getBoolean("online");
                    Long lastSeen = doc.getLong("lastSeen");
                    
                    // A user is truly online if they are marked online AND were seen in the last 1 minute
                    boolean isActuallyOnline = online != null && online && 
                            (lastSeen == null || (System.currentTimeMillis() - lastSeen < 60000));

                    if (isActuallyOnline) {
                        binding.chatStatus.setText("Online");
                        binding.chatStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                        binding.chatOnlineDot.setVisibility(android.view.View.VISIBLE);
                    } else if (lastSeen != null) {
                        String timeAgo = PetTimeUtils.getTimeAgo(new java.util.Date(lastSeen));
                        binding.chatStatus.setText("Active " + timeAgo);
                        binding.chatStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                        binding.chatOnlineDot.setVisibility(android.view.View.GONE);
                    } else {
                        binding.chatStatus.setText("Offline");
                        binding.chatStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                        binding.chatOnlineDot.setVisibility(android.view.View.GONE);
                    }
                });
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
                                    
                                    // If message is from other user, mark it as read immediately
                                    if (!message.isSentByMe()) {
                                        markAsRead();
                                    }
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

        // Reset read status for the receiver
        Map<String, Object> readStatus = new HashMap<>();
        readStatus.put(currentUserId, true);
        readStatus.put(otherUserId, false);
        conversation.put("readStatus", readStatus);

        db.collection("conversations").document(conversationId)
                .set(conversation, SetOptions.merge());
    }

    @Override
    protected void onStart() {
        super.onStart();
        activeChatUserId = otherUserId;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (otherUserId != null && otherUserId.equals(activeChatUserId)) {
            activeChatUserId = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
        if (presenceListener != null) presenceListener.remove();
    }
}
