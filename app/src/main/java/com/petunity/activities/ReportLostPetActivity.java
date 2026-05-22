package com.petunity.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.petunity.models.ImageValidator;
import com.petunity.models.PetListing;
import com.petunity.models.UserManager;
import com.petunity.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ReportLostPetActivity extends AppCompatActivity {
    private static final String TAG = "ReportLostPetActivity";
    private TextInputEditText petNameInput, breedInput, locationInput, descriptionInput, rewardInput, contactInput;
    private TextInputLayout rewardInputLayout;
    private ImageView petImageView;
    private View addPhotoLayout;
    private View loadingOverlay;
    private MaterialButton submitButton;

    private Uri selectedImageUri;
    private boolean isLostReport;
    private boolean isPetValidated = false;

    private final ActivityResultLauncher<String[]> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    validateAndLoadImage(uri);
                }
            }
    );

    private void validateAndLoadImage(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap != null) {
                setLoading(true);
                ImageValidator.validateIsPet(this, bitmap, new ImageValidator.ValidationCallback() {
                    @Override
                    public void onResult(boolean isPet) {
                        setLoading(false);
                        if (isPet) {
                            isPetValidated = true;
                            selectedImageUri = uri;
                            petImageView.setImageBitmap(bitmap);
                            petImageView.setVisibility(View.VISIBLE);
                            addPhotoLayout.setVisibility(View.GONE);
                        } else {
                            isPetValidated = false;
                            selectedImageUri = null;
                            Toast.makeText(ReportLostPetActivity.this, "no pet detected please try again", Toast.LENGTH_LONG).show();
                            petImageView.setVisibility(View.GONE);
                            addPhotoLayout.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        Log.e(TAG, "Validation error", e);
                        Toast.makeText(ReportLostPetActivity.this, "Error validating image", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

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
        loadingOverlay = findViewById(R.id.loadingOverlay);
        submitButton = findViewById(R.id.submitReportButton);

        if (!isLostReport && rewardInputLayout != null) {
            rewardInputLayout.setVisibility(View.GONE);
        }

        findViewById(R.id.photoCard).setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));

        submitButton.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        if (selectedImageUri == null || !isPetValidated) {
            Toast.makeText(this, "A valid photo of the pet is required", Toast.LENGTH_SHORT).show();
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
        if (loadingOverlay != null) loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!loading);
    }

    private void uploadToCloudinary() {
        try {
            File compressedFile = ImageUtils.compressImage(this, selectedImageUri, "report_" + System.currentTimeMillis() + ".jpg");
            MediaManager.get().upload(compressedFile.getAbsolutePath())
                    .unsigned("ml_defaults")
                    .callback(new UploadCallback() {
                        @Override public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            saveReportToFirestore(imageUrl);
                            if (compressedFile.exists()) compressedFile.delete();
                        }
                        @Override public void onError(String requestId, ErrorInfo error) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(ReportLostPetActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                            });
                            if (compressedFile.exists()) compressedFile.delete();
                        }
                        @Override public void onStart(String requestId) {}
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } catch (IOException e) {
            setLoading(false);
            Toast.makeText(this, "Failed to compress image", Toast.LENGTH_SHORT).show();
        }
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
