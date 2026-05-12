package com.petunity.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.fragments.AlertFragment;
import com.petunity.fragments.ChatFragment;
import com.petunity.fragments.FindFragment;
import com.petunity.fragments.HomeFragment;
import com.petunity.fragments.ProfileFragment;

public class RescuerMainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private MaterialCardView profileIconCard;
    private ImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescuer_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        profileIconCard = findViewById(R.id.profileIconCard);
        profileIcon = findViewById(R.id.profileIcon);

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