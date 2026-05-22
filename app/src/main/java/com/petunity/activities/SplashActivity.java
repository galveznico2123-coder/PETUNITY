package com.petunity.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.models.UserManager;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
            boolean autoLogin = prefs.getBoolean(LoginActivity.PREF_AUTO_LOGIN, true);

            if (currentUser != null && autoLogin) {
                fetchUserDataAndNavigate(currentUser);
            } else {
                navigateToLogin();
            }
        }, 3000);
    }

    private void fetchUserDataAndNavigate(FirebaseUser user) {
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing()) return;
                    if (doc.exists()) {
                        populateUserManager(doc);
                        
                        Boolean profileCompleted = doc.getBoolean("profileCompleted");
                        if (Boolean.TRUE.equals(profileCompleted)) {
                            navigateToMain();
                        } else {
                            // If profile wasn't finished, go to onboarding
                            Intent intent = new Intent(SplashActivity.this, CompleteProfileActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        navigateToMain();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    Log.e(TAG, "Error fetching user data", e);
                    navigateToMain();
                });
    }

    private void populateUserManager(DocumentSnapshot doc) {
        UserManager um = UserManager.getInstance();
        um.setName(doc.getString("name"));
        um.setEmail(doc.getString("email"));
        um.setPhone(doc.getString("phone"));
        um.setLocation(doc.getString("location"));
        um.setBio(doc.getString("bio"));
        um.setProfileImageUrl(doc.getString("profileImageUrl"));
        
        String membership = doc.getString("membershipType");
        if (membership != null) um.setMembershipType(membership);
        
        Double helped = doc.getDouble("petsHelped");
        if (helped != null) um.setPetsHelped(helped);
    }

    private void navigateToMain() {
        Intent intent;
        if (UserManager.getInstance().isRescuer()) {
            intent = new Intent(SplashActivity.this, RescuerMainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
