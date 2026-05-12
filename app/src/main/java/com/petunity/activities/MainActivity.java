package com.petunity.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
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
import com.petunity.fragments.PlayFragment;
import com.petunity.fragments.ProfileFragment;
import com.petunity.models.Post;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigationView;
    private MaterialCardView profileIconCard;
    private ImageView profileIcon;
    private ListenerRegistration urgentAlertListener;
    private long sessionStartTime;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean notificationGranted = result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);
                
                if (notificationGranted != null && notificationGranted) {
                    Log.d(TAG, "Notification permission granted");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sessionStartTime = System.currentTimeMillis();

        checkPermissions();

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setItemIconTintList(null);
        profileIconCard = findViewById(R.id.profileIconCard);
        profileIcon = findViewById(R.id.profileIcon);

        handleIntent(getIntent());
        loadProfileImage();

        profileIconCard.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new ProfileFragment())
                    .commit();
            bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
            for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                bottomNavigationView.getMenu().getItem(i).setChecked(false);
            }
            bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_alert) {
                selectedFragment = new AlertFragment();
            } else if (itemId == R.id.navigation_find) {
                selectedFragment = new FindFragment();
            } else if (itemId == R.id.navigation_play) {
                selectedFragment = new com.petunity.fragments.PlayFragment();
            } else if (itemId == R.id.navigation_chat) {
                selectedFragment = new ChatFragment();
            } else {
                selectedFragment = new HomeFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment)
                    .commit();
            return true;
        });

        listenForUrgentAlerts();
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

    private void listenForUrgentAlerts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getUid();

        urgentAlertListener = db.collection("posts")
                .whereEqualTo("urgent", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Urgent Listener failed.", error);
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Post post = dc.getDocument().toObject(Post.class);
                                
                                if (post.getTimestamp() != null && 
                                    post.getTimestamp().toDate().getTime() > sessionStartTime &&
                                    post.getUserId() != null && 
                                    !post.getUserId().equals(currentUserId)) {
                                    showUrgentNotification(post);
                                }
                            }
                        }
                    }
                });
    }

    private void showUrgentNotification(Post post) {
        String channelId = "urgent_alerts";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Urgent Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.petunity_logo)
                .setContentTitle("URGENT: " + post.getTitle())
                .setContentText(post.getUserName() + " needs help: " + post.getContent())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "chat".equals(intent.getStringExtra("open_fragment"))) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new ChatFragment())
                    .commit();
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
            }
        } else if (getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        }
    }

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
        requestPermissionLauncher.launch(permissions);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (urgentAlertListener != null) {
            urgentAlertListener.remove();
        }
    }
}