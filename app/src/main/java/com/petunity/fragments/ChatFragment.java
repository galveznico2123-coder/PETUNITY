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
    private final java.util.Map<String, ListenerRegistration> userListeners = new java.util.HashMap<>();

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
        }

        EditText searchInput = view.findViewById(R.id.searchInput);
        if (searchInput != null) {
            searchInput.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "Search feature coming soon!", Toast.LENGTH_SHORT).show()
            );
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
                        conversations.clear();
                        recentUsers.clear();
                        
                        java.util.Set<String> activeUserIds = new java.util.HashSet<>();
                        
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            try {
                                Conversation conv = doc.toObject(Conversation.class);
                                
                                String foundOtherId = null;
                                List<String> participants = conv.getParticipants();
                                if (participants != null) {
                                    for (String uid : participants) {
                                        if (!uid.equals(currentUserId)) {
                                            foundOtherId = uid;
                                            break;
                                        }
                                    }
                                }
                                
                                if (foundOtherId == null) continue;
                                conv.setOtherUserId(foundOtherId);
                                activeUserIds.add(foundOtherId);

                                if (conv.getNames() != null && conv.getNames().containsKey(foundOtherId)) {
                                    String name = conv.getNames().get(foundOtherId);
                                    conv.setName(name);
                                }

                                conversations.add(conv);
                                
                                // Setup or reuse listener for this user
                                listenToUserDetails(foundOtherId);

                                // Add to horizontal bubble list
                                User recentUser = new User();
                                recentUser.setId(foundOtherId);
                                recentUsers.add(recentUser);

                            } catch (Exception e) {
                                Log.e(TAG, "Error processing conversation", e);
                            }
                        }
                        
                        // Clean up listeners for users no longer in conversations
                        java.util.Iterator<java.util.Map.Entry<String, ListenerRegistration>> it = userListeners.entrySet().iterator();
                        while (it.hasNext()) {
                            java.util.Map.Entry<String, ListenerRegistration> entry = it.next();
                            if (!activeUserIds.contains(entry.getKey())) {
                                entry.getValue().remove();
                                it.remove();
                            }
                        }

                        adapter.notifyDataSetChanged();
                        onlineUsersAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void listenToUserDetails(String userId) {
        if (userListeners.containsKey(userId)) {
            return;
        }

        ListenerRegistration listener = db.collection("users").document(userId)
                .addSnapshotListener((doc, error) -> {
                    if (doc != null && doc.exists()) {
                        String name = doc.getString("name");
                        String avatarUrl = doc.getString("profileImageUrl");
                        Boolean isOnline = doc.getBoolean("online");
                        Long lastSeen = doc.getLong("lastSeen");
                        
                        boolean actuallyOnline = isOnline != null && isOnline && 
                                (lastSeen == null || (System.currentTimeMillis() - lastSeen < 60000)); // 1 min threshold

                        // Update in conversations list
                        for (Conversation c : conversations) {
                            if (c.getOtherUserId() != null && c.getOtherUserId().equals(userId)) {
                                if (name != null) c.setName(name);
                                if (avatarUrl != null) c.setAvatarUrl(avatarUrl);
                                c.setOnline(actuallyOnline);
                            }
                        }

                        // Update in recent bubbles list
                        for (User u : recentUsers) {
                            if (u.getId() != null && u.getId().equals(userId)) {
                                if (name != null) u.setName(name);
                                if (avatarUrl != null) u.setProfileImageUrl(avatarUrl);
                                u.setOnline(actuallyOnline);
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                onlineUsersAdapter.notifyDataSetChanged();
                            });
                        }
                    }
                });
        userListeners.put(userId, listener);
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
        if (conversationListener != null) conversationListener.remove();
        for (ListenerRegistration lr : userListeners.values()) {
            lr.remove();
        }
        userListeners.clear();
    }
}
