package com.petunity.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.petunity.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoImage = findViewById(R.id.logoImage);

        if (logoImage != null) {
            AnimationSet animationSet = new AnimationSet(true);

            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(1200);
            fadeIn.setFillAfter(true);

            ScaleAnimation scaleAnimation = new ScaleAnimation(
                    0.9f, 1f, 0.9f, 1f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(1000);
            scaleAnimation.setFillAfter(true);

            animationSet.addAnimation(fadeIn);
            animationSet.addAnimation(scaleAnimation);
            logoImage.startAnimation(animationSet);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            
            Intent intent;
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                // If logged in, we let LoginActivity handle the redirection logic 
                // or go straight to MainActivity if we want to be faster.
                // For now, staying consistent with existing logic but making it safer.
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2500);
    }
}