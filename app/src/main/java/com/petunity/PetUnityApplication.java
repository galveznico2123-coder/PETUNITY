package com.petunity;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cloudinary.android.MediaManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.petunity.utils.PresenceManager;

import java.util.HashMap;
import java.util.Map;

public class PetUnityApplication extends Application {
    private int activityCount = 0;
    private final android.os.Handler heartbeatHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (activityCount > 0) {
                PresenceManager.updateStatus(true);
                heartbeatHandler.postDelayed(this, 30000); // Heartbeat every 30 seconds
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        
        // ... (existing firebase/cloudinary init) ...
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dyfqawl6s");
        try {
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (activityCount == 0) {
                    PresenceManager.updateStatus(true);
                    heartbeatHandler.post(heartbeatRunnable);
                }
                activityCount++;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                    PresenceManager.updateStatus(false);
                    heartbeatHandler.removeCallbacks(heartbeatRunnable);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}
