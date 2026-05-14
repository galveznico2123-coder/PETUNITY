package com.petunity.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.petunity.R;
import com.petunity.databinding.ActivityMainBinding;
import com.petunity.fragments.AlertFragment;
import com.petunity.fragments.ChatFragment;
import com.petunity.fragments.FindFragment;
import com.petunity.fragments.HomeFragment;
import com.petunity.fragments.PlayFragment;
import com.petunity.fragments.ProfileFragment;
import com.petunity.models.Post;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // Intent Keys
    public static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    public static final String FRAGMENT_CHAT = "chat";
    public static final String FRAGMENT_ALERT = "alert";
    
    // Notification Constants
    private static final String CHANNEL_ID_URGENT = "urgent_alerts";

    private ActivityMainBinding binding;
    private ListenerRegistration urgentAlertListener;
    private ConnectivityManager.NetworkCallback networkCallback;
    private long sessionStartTime;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (Map<String, Boolean> result) -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.POST_NOTIFICATIONS))) {
                    Log.d(TAG, "Notification permission granted");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        sessionStartTime = System.currentTimeMillis();
        checkPermissions();
        setupNetworkListener();

        // Setup UI Components via binding
        binding.bottomNavigationView.setItemIconTintList(null);

        handleIntent(getIntent());
        loadProfileImage();

        binding.profileIconCard.setOnClickListener(v -> {
            loadFragment(new ProfileFragment());
            uncheckBottomNav();
        });

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_alert) {
                selectedFragment = new AlertFragment();
            } else if (itemId == R.id.navigation_find) {
                selectedFragment = new FindFragment();
            } else if (itemId == R.id.navigation_play) {
                selectedFragment = new PlayFragment();
            } else if (itemId == R.id.navigation_chat) {
                selectedFragment = new ChatFragment();
            } else {
                selectedFragment = new HomeFragment();
            }

            loadFragment(selectedFragment);
            return true;
        });

        listenForUrgentAlerts();
    }

    private void setupNetworkListener() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        Snackbar.make(binding.getRoot(), "Network connection lost", Snackbar.LENGTH_INDEFINITE)
                                .setAction("Dismiss", v -> {})
                                .show();
                    }
                });
            }

            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        Toast.makeText(MainActivity.this, "Back online!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        cm.registerNetworkCallback(request, networkCallback);
    }

    private void uncheckBottomNav() {
        binding.bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < binding.bottomNavigationView.getMenu().size(); i++) {
            binding.bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        binding.bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
    }

    private void loadFragment(Fragment fragment) {
        if (isFinishing() || fragment == null) return;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    private void loadProfileImage() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing()) return;
                    String url = doc.getString("profileImageUrl");
                    if (url != null && !url.isEmpty()) {
                        Glide.with(this)
                                .load(url)
                                .circleCrop()
                                .placeholder(R.drawable.ic_user)
                                .into(binding.profileIcon);
                    } else {
                        binding.profileIcon.setImageResource(R.drawable.ic_user);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading profile image", e));
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
                                if (post != null && post.getTimestamp() != null && 
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
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_URGENT, "Urgent Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_URGENT)
                .setSmallIcon(R.drawable.petunity_logo)
                .setContentTitle("URGENT: " + post.getTitle())
                .setContentText(post.getUserName() + " needs help: " + post.getContent())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String openFragment = intent.getStringExtra(EXTRA_OPEN_FRAGMENT);
        
        if (FRAGMENT_CHAT.equals(openFragment)) {
            loadFragment(new ChatFragment());
            binding.bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
        } else if (FRAGMENT_ALERT.equals(openFragment)) {
            loadFragment(new AlertFragment());
            binding.bottomNavigationView.setSelectedItemId(R.id.navigation_alert);
        } else if (getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment) == null) {
            loadFragment(new HomeFragment());
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
        if (urgentAlertListener != null) urgentAlertListener.remove();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) cm.unregisterNetworkCallback(networkCallback);
        }
        binding = null;
    }
}
