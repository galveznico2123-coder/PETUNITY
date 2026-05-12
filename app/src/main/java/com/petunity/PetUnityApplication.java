package com.petunity;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class PetUnityApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Cloudinary globally with your Cloud Name
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dyfqawl6s");
        try {
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }
}
