package com.petunity.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.models.PetListing;
import com.petunity.models.UserManager;

import java.util.Map;

public class ReportLostPetActivity extends AppCompatActivity {
    private TextInputEditText petNameInput, breedInput, locationInput, descriptionInput, rewardInput, contactInput;
    private TextInputLayout rewardInputLayout;
    private ImageView petImageView;
    private View addPhotoLayout;
    private ProgressBar progressBar;
    private MaterialButton submitButton;

    private Uri selectedImageUri;
    private boolean isLostReport;

    private final ActivityResultLauncher<String[]> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    petImageView.setImageURI(selectedImageUri);
                    petImageView.setVisibility(View.VISIBLE);
                    addPhotoLayout.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_lost_pet);

        isLostReport = getIntent().getBooleanExtra("is_lost", true);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isLostReport ? "Broadcast Lost Pet" : "Broadcast Found Pet");
        }

        petNameInput = findViewById(R.id.petNameInput);
        breedInput = findViewById(R.id.breedInput);
        locationInput = findViewById(R.id.locationInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        rewardInput = findViewById(R.id.rewardInput);
        rewardInputLayout = findViewById(R.id.rewardInputLayout);
        contactInput = findViewById(R.id.contactInput);
        petImageView = findViewById(R.id.petImageView);
        addPhotoLayout = findViewById(R.id.addPhotoLayout);
        progressBar = findViewById(R.id.loadingProgressBar);
        submitButton = findViewById(R.id.submitReportButton);

        // Found pets don't usually offer rewards
        if (!isLostReport && rewardInputLayout != null) {
            rewardInputLayout.setVisibility(View.GONE);
        }

        findViewById(R.id.photoCard).setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));

        submitButton.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "A photo of the pet is required for the alert", Toast.LENGTH_SHORT).show();
            return;
        }

        String location = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
        String contact = contactInput.getText() != null ? contactInput.getText().toString().trim() : "";

        if (location.isEmpty()) {
            locationInput.setError("Incident location is required");
            return;
        }
        if (contact.isEmpty()) {
            contactInput.setError("Contact number is required for recovery");
            return;
        }

        setLoading(true);
        uploadToCloudinary();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!loading);
        submitButton.setText(loading ? "Broadcasting Alert..." : "Broadcast Emergency Report");
    }

    private void uploadToCloudinary() {
        MediaManager.get().upload(selectedImageUri)
                .unsigned("ml_defaults")
                .callback(new UploadCallback() {
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveReportToFirestore(imageUrl);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(ReportLostPetActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveReportToFirestore(String imageUrl) {
        String name = petNameInput.getText().toString().trim();
        String breed = breedInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String desc = descriptionInput.getText().toString().trim();
        String reward = rewardInput != null ? rewardInput.getText().toString().trim() : "";
        String contact = contactInput.getText().toString().trim();

        if (name.isEmpty()) name = isLostReport ? "Lost Pet" : "Found Pet";
        
        PetListing report = new PetListing(name, breed, location, "", 
                isLostReport ? "lost" : "found", UserManager.getInstance().getName(), 
                FirebaseAuth.getInstance().getUid(), imageUrl);
        
        report.setDescription(desc);
        report.setReward(reward);
        report.setContactPhone(contact);
        report.setTimestamp(Timestamp.now());

        FirebaseFirestore.getInstance().collection("pet_listing")
                .add(report)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Emergency Alert Broadcasted!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Network Error. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
