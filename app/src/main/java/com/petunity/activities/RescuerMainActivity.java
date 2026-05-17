package com.petunity.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.petunity.R;
import com.petunity.fragments.AlertFragment;
import com.petunity.fragments.ChatFragment;
import com.petunity.fragments.FindFragment;
import com.petunity.fragments.HomeFragment;
import com.petunity.fragments.ProfileFragment;
import com.petunity.models.Conversation;

public class RescuerMainActivity extends AppCompatActivity {
    private static final String TAG = "RescuerMainActivity";
    private static final String CHANNEL_ID_CHAT = "chat_notifications";

    private BottomNavigationView bottomNavigationView;
    private MaterialCardView profileIconCard;
    private ImageView profileIcon;
    private ListenerRegistration messageListener;
    private long sessionStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescuer_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        profileIconCard = findViewById(R.id.profileIconCard);
        profileIcon = findViewById(R.id.profileIcon);

        sessionStartTime = System.currentTimeMillis();

        // For rescuers, we start with the Find/Map view to see pets in need
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new FindFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.navigation_find);
        }

        loadProfileImage();

        profileIconCard.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new ProfileFragment())
                    .commit();
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_find) {
                selectedFragment = new FindFragment();
            } else if (itemId == R.id.navigation_rescue_feed) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_alerts) {
                selectedFragment = new AlertFragment();
            } else if (itemId == R.id.navigation_chat) {
                selectedFragment = new ChatFragment();
            } else {
                selectedFragment = new FindFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment)
                    .commit();
            return true;
        });

        listenForNewMessages();
    }

    private void listenForNewMessages() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        messageListener = db.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Message Listener failed.", error);
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED || dc.getType() == DocumentChange.Type.ADDED) {
                                Conversation conv = dc.getDocument().toObject(Conversation.class);
                                
                                if (conv != null && conv.getLastSenderId() != null && 
                                    !conv.getLastSenderId().equals(currentUserId) &&
                                    conv.getLastTimestampAsDate() != null &&
                                    conv.getLastTimestampAsDate().toDate().getTime() > sessionStartTime &&
                                    !conv.getLastSenderId().equals(ChatActivity.activeChatUserId)) {
                                    
                                    showChatNotification(conv);
                                }
                            }
                        }
                    }
                });
    }

    private void showChatNotification(Conversation conv) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_CHAT, "New Messages", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("other_user_id", conv.getLastSenderId());
        intent.putExtra("user_name", conv.getLastSenderName());
        intent.putExtra("avatar_url", conv.getAvatarUrl());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_CHAT)
                .setSmallIcon(R.drawable.logo_pet)
                .setContentTitle(conv.getLastSenderName())
                .setContentText(conv.getLastMessage())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify(conv.getLastSenderId().hashCode(), builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }

    private void loadProfileImage() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null && profileIcon != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String url = doc.getString("profileImageUrl");
                    if (url != null && !url.isEmpty()) {
                        Glide.with(this).load(url).circleCrop().placeholder(R.drawable.ic_user).into(profileIcon);
                    } else {
                        profileIcon.setImageResource(R.drawable.ic_user);
                    }
                });
        }
    }
}