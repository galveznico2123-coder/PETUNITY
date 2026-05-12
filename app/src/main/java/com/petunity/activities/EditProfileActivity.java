package com.petunity.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.models.UserManager;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextInputEditText nameInput, emailInput, phoneInput, locationInput, bioInput;
    private ProgressBar progressBar;
    private MaterialButton saveButton;
    
    private Uri selectedImageUri;
    private String currentImageUrl;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        profileImage = findViewById(R.id.editProfileImage);
        nameInput = findViewById(R.id.nameEditText);
        emailInput = findViewById(R.id.emailEditText);
        phoneInput = findViewById(R.id.phoneEditText);
        locationInput = findViewById(R.id.locationEditText);
        bioInput = findViewById(R.id.bioEditText);
        
        progressBar = findViewById(R.id.saveProgressBar);
        saveButton = findViewById(R.id.btnSaveProfile);

        loadUserData();

        View.OnClickListener photoClick = v -> getImage.launch(new String[]{"image/*"});
        profileImage.setOnClickListener(photoClick);
        findViewById(R.id.btnChangePhoto).setOnClickListener(photoClick);
        
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        // Pre-fill with manager data if available
        nameInput.setText(UserManager.getInstance().getName());

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");
                        String location = doc.getString("location");
                        String bio = doc.getString("bio");
                        currentImageUrl = doc.getString("profileImageUrl");
                        
                        if (name != null) nameInput.setText(name);
                        if (email != null) emailInput.setText(email);
                        if (phone != null) phoneInput.setText(phone);
                        if (location != null) locationInput.setText(location);
                        if (bio != null) bioInput.setText(bio);
                        
                        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                            Glide.with(this).load(currentImageUrl).circleCrop().into(profileImage);
                        }
                    }
                });
    }

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String bio = bioInput.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (selectedImageUri != null) {
            uploadToCloudinary(name, email, phone, location, bio);
        } else {
            updateFirestore(name, email, phone, location, bio, currentImageUrl);
        }
    }

    private void uploadToCloudinary(String name, String email, String phone, String location, String bio) {
        MediaManager.get().upload(selectedImageUri)
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
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        setLoading(false);
                        Toast.makeText(EditProfileActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void updateFirestore(String name, String email, String phone, String location, String bio, String imageUrl) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("location", location);
        updates.put("bio", bio);
        if (imageUrl != null) updates.put("profileImageUrl", imageUrl);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    UserManager.getInstance().setName(name);
                    setLoading(false);
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        nameInput.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        phoneInput.setEnabled(!loading);
        locationInput.setEnabled(!loading);
        bioInput.setEnabled(!loading);
        profileImage.setEnabled(!loading);
    }
}
