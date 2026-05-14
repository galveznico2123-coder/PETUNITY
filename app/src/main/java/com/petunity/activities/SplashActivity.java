package com.petunity.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.petunity.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load the looping animation layout immediately
        // The background is set to #FFE0B2 in XML to match the system splash
        setContentView(R.layout.activity_splash);

        // Transition to LoginActivity after 3 seconds as requested
        // Even if the user is already logged in, we show the Login screen first
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3000);
    }
}
