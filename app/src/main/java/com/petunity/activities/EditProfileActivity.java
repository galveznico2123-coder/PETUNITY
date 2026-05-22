package com.petunity.activities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.petunity.R;
import com.petunity.models.UserManager;
import com.petunity.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextInputEditText nameInput, emailInput, phoneInput, locationInput, bioInput;
    private View loadingOverlay;
    private MaterialButton saveButton;
    
    private Uri selectedImageUri;
    private String currentImageUrl;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private UserManager userManager;

    private final ActivityResultLauncher<String[]> getImage = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(profileImage);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userManager = UserManager.getInstance();

        setupToolbar();
        initViews();
        loadInitialData();
        setupListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        profileImage = findViewById(R.id.editProfileImage);
        nameInput = findViewById(R.id.nameEditText);
        emailInput = findViewById(R.id.emailEditText);
        phoneInput = findViewById(R.id.phoneEditText);
        locationInput = findViewById(R.id.locationEditText);
        bioInput = findViewById(R.id.bioEditText);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        saveButton = findViewById(R.id.btnSaveProfile);
    }

    private void loadInitialData() {
        nameInput.setText(userManager.getName());
        emailInput.setText(userManager.getEmail());
        phoneInput.setText(userManager.getPhone());
        locationInput.setText(userManager.getLocation());
        bioInput.setText(userManager.getBio());
        currentImageUrl = userManager.getProfileImageUrl();

        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Glide.with(this).load(currentImageUrl).circleCrop().placeholder(R.drawable.ic_user).into(profileImage);
        }

        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");
                        String location = doc.getString("location");
                        String bio = doc.getString("bio");
                        String imageUrl = doc.getString("profileImageUrl");

                        if (name != null) nameInput.setText(name);
                        if (email != null) emailInput.setText(email);
                        if (phone != null) phoneInput.setText(phone);
                        if (location != null) locationInput.setText(location);
                        if (bio != null) bioInput.setText(bio);
                        
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            currentImageUrl = imageUrl;
                            Glide.with(this).load(imageUrl).circleCrop().placeholder(R.drawable.ic_user).into(profileImage);
                        }
                    }
                });
    }

    private void setupListeners() {
        View.OnClickListener photoClick = v -> getImage.launch(new String[]{"image/*"});
        profileImage.setOnClickListener(photoClick);
        findViewById(R.id.btnChangePhoto).setOnClickListener(photoClick);
        saveButton.setOnClickListener(v -> validateAndSave());
    }

    private void validateAndSave() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String bio = bioInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Name is required");
            return;
        }

        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Invalid email format");
            return;
        }

        setLoading(true);

        if (selectedImageUri != null) {
            uploadImageAndSave(name, email, phone, location, bio);
        } else {
            updateFirestore(name, email, phone, location, bio, currentImageUrl);
        }
    }

    private void uploadImageAndSave(String name, String email, String phone, String location, String bio) {
        try {
            File compressedFile = ImageUtils.compressImage(this, selectedImageUri, "profile_" + System.currentTimeMillis() + ".jpg");
            
            MediaManager.get().upload(compressedFile.getAbsolutePath())
                    .unsigned("ml_defaults")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            updateFirestore(name, email, phone, location, bio, imageUrl);
                            if (compressedFile.exists()) compressedFile.delete();
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(EditProfileActivity.this, "Image upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                                if (compressedFile.exists()) compressedFile.delete();
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } catch (IOException e) {
            setLoading(false);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFirestore(String name, String email, String phone, String location, String bio, String imageUrl) {
        String uid = mAuth.getUid();
        if (uid == null) {
            setLoading(false);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("location", location);
        updates.put("bio", bio);
        if (imageUrl != null) updates.put("profileImageUrl", imageUrl);

        db.collection("users").document(uid).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    userManager.setName(name);
                    userManager.setEmail(email);
                    userManager.setPhone(phone);
                    userManager.setLocation(location);
                    userManager.setBio(bio);
                    if (imageUrl != null) userManager.setProfileImageUrl(imageUrl);

                    setLoading(false);
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        nameInput.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        phoneInput.setEnabled(!loading);
        locationInput.setEnabled(!loading);
        bioInput.setEnabled(!loading);
        findViewById(R.id.btnChangePhoto).setEnabled(!loading);
    }
}
