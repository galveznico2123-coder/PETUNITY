package com.petunity.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.petunity.R;
import com.petunity.models.UserManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private TextInputEditText phoneInput, locationInput, bioInput;
    private MaterialButton btnFinish;
    private View loadingOverlay;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocation != null && fineLocation || coarseLocation != null && coarseLocation) {
                    detectLocation();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        phoneInput = findViewById(R.id.phoneInput);
        locationInput = findViewById(R.id.locationInput);
        bioInput = findViewById(R.id.bioInput);
        btnFinish = findViewById(R.id.btnFinish);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        checkLocationPermissions();

        btnFinish.setOnClickListener(v -> saveProfileAndContinue());
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void detectLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            String city = addresses.get(0).getLocality();
                            String state = addresses.get(0).getAdminArea();
                            String fullLocation = (city != null ? city : "") + (city != null && state != null ? ", " : "") + (state != null ? state : "");
                            locationInput.setText(fullLocation);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void saveProfileAndContinue() {
        String phone = phoneInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String bio = bioInput.getText().toString().trim();

        if (phone.isEmpty()) {
            phoneInput.setError("Phone number is required");
            return;
        }

        setLoading(true);

        String uid = mAuth.getUid();
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", phone);
        updates.put("location", location);
        updates.put("bio", bio);
        updates.put("profileCompleted", true);

        db.collection("users").document(uid).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    UserManager um = UserManager.getInstance();
                    um.setPhone(phone);
                    um.setLocation(location);
                    um.setBio(bio);

                    setLoading(false);
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        btnFinish.setEnabled(!loading);
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, UserManager.getInstance().isRescuer() ? RescuerMainActivity.class : MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
