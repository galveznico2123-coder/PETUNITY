package com.petunity.fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.petunity.R;
import com.petunity.activities.ChatActivity;
import com.petunity.activities.MainActivity;
import com.petunity.adapters.MessagesAdapter;
import com.petunity.adapters.OnlineUsersAdapter;
import com.petunity.models.Conversation;
import com.petunity.models.User;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    private MessagesAdapter adapter;
    private List<Conversation> conversations;
    private OnlineUsersAdapter onlineUsersAdapter;
    private List<User> recentUsers;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration conversationListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup Conversations (Vertical List)
        RecyclerView recyclerView = view.findViewById(R.id.conversationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        conversations = new ArrayList<>();
        adapter = new MessagesAdapter(conversations);
        recyclerView.setAdapter(adapter);

        // Setup "Recent Interaction" Users (Horizontal Bubbles)
        RecyclerView onlineUsersRecyclerView = view.findViewById(R.id.onlineUsersRecyclerView);
        onlineUsersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recentUsers = new ArrayList<>();
        onlineUsersAdapter = new OnlineUsersAdapter(recentUsers, user -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("other_user_id", user.getId());
            intent.putExtra("user_name", user.getName());
            intent.putExtra("avatar_url", user.getProfileImageUrl());
            startActivity(intent);
        });
        onlineUsersRecyclerView.setAdapter(onlineUsersAdapter);

        if (mAuth.getCurrentUser() != null) {
            loadConversationsAndRecentUsers();
            updateMyStatus(true);
        }

        EditText searchInput = view.findViewById(R.id.searchInput);
        if (searchInput != null) {
            searchInput.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Search feature coming soon!", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void updateMyStatus(boolean online) {
        String uid = mAuth.getUid();
        if (uid != null) {
            db.collection("users").document(uid).update("online", online);
        }
    }

    private void loadConversationsAndRecentUsers() {
        String currentUserId = mAuth.getUid();
        if (currentUserId == null) return;

        conversationListener = db.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        // Clear lists to refresh with only interacted users
                        conversations.clear();
                        recentUsers.clear();
                        
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            try {
                                Conversation conv = doc.toObject(Conversation.class);
                                
                                String otherId = null;
                                if (conv.getParticipants() != null) {
                                    for (String uid : conv.getParticipants()) {
                                        if (!uid.equals(currentUserId)) {
                                            otherId = uid;
                                            break;
                                        }
                                    }
                                }
                                
                                if (otherId == null) continue;
                                conv.setOtherUserId(otherId);

                                // Set initial data from conversation doc
                                if (conv.getNames() != null && conv.getNames().containsKey(otherId)) {
                                    conv.setName(conv.getNames().get(otherId));
                                }

                                conversations.add(conv);
                                fetchUserDetailsForConversation(conv); // Fetch live avatar and name
                                
                                // Add to horizontal bubble list
                                User recentUser = new User();
                                recentUser.setId(otherId);
                                fetchUserDetailsForBubble(recentUser);

                            } catch (Exception e) {
                                Log.e(TAG, "Error processing conversation", e);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        onlineUsersAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void fetchUserDetailsForConversation(Conversation conv) {
        db.collection("users").document(conv.getOtherUserId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String avatarUrl = doc.getString("profileImageUrl");
                        if (name != null) conv.setName(name);
                        if (avatarUrl != null) conv.setAvatarUrl(avatarUrl);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void fetchUserDetailsForBubble(User user) {
        db.collection("users").document(user.getId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        user.setName(doc.getString("name"));
                        user.setProfileImageUrl(doc.getString("profileImageUrl"));
                        user.setOnline(doc.getBoolean("online") != null && doc.getBoolean("online"));
                        
                        // Check if already in list to avoid duplicates
                        boolean exists = false;
                        for (User u : recentUsers) {
                            if (u.getId().equals(user.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            recentUsers.add(user);
                            onlineUsersAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void showNotification(String senderName, String message) {
        if (getContext() == null) return;

        String channelId = "chat_notifications";
        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Chat Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.putExtra("open_fragment", "chat");
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), channelId)
                .setSmallIcon(R.drawable.logo_pet)
                .setContentTitle(senderName)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify(1, builder.build());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        updateMyStatus(false);
        if (conversationListener != null) conversationListener.remove();
    }
}
