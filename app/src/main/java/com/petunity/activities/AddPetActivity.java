package com.petunity.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.databinding.ActivityAddPetBinding;
import com.petunity.models.ImageValidator;
import com.petunity.models.PetProfile;
import com.petunity.models.UserManager;
import com.google.android.material.chip.Chip;
import com.petunity.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddPetActivity extends AppCompatActivity {
    private static final String TAG = "AddPetActivity";
    private static final String DEFAULT_LOCATION = "Nearby";

    private ActivityAddPetBinding binding;
    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    private String detectedLocation = DEFAULT_LOCATION;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String[]> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    try {
                        // Take persistable URI permission for long-term access
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException e) {
                            Log.w(TAG, "Could not take persistable permission", e);
                        }
                        
                        // Load bitmap safely with scaling if needed
                        selectedBitmap = loadScaledBitmap(uri);

                        if (selectedBitmap != null) {
                            binding.petImageView.setImageBitmap(selectedBitmap);
                            validateImage();
                        } else {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading image", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private Bitmap loadScaledBitmap(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) return null;
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        is.close();

        int targetW = 800;
        int targetH = 800;
        int scale = Math.max(1, Math.min(options.outWidth / targetW, options.outHeight / targetH));

        options.inJustDecodeBounds = false;
        options.inSampleSize = scale;
        
        is = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
        if (is != null) is.close();
        return bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddPetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        detectLocation();

        binding.selectImageButton.setOnClickListener(v -> {
            imagePickerLauncher.launch(new String[]{"image/*"});
        });

        binding.savePetButton.setOnClickListener(v -> performSave());
    }

    private void performSave() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String name = binding.petNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            binding.petNameInput.setError("Pet name is required");
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a photo of your pet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        uploadToCloudinary();
    }

    private void setLoading(boolean loading) {
        binding.savePetButton.setEnabled(!loading);
        binding.selectImageButton.setEnabled(!loading);
        binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.savePetButton.setText(R.string.finalizing_persona);
        } else {
            binding.savePetButton.setText(R.string.create_persona);
        }
    }

    private void detectLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            detectedLocation = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea();
                        }
                    } catch (IOException e) {
                        detectedLocation = DEFAULT_LOCATION;
                    }
                }
            });
        }
    }

    private void validateImage() {
        binding.savePetButton.setEnabled(false);
        binding.savePetButton.setText(R.string.validating);

        ImageValidator.validateIsPet(this, selectedBitmap, new ImageValidator.ValidationCallback() {
            @Override
            public void onResult(boolean isPet) {
                if (isFinishing()) return;
                binding.savePetButton.setEnabled(true);
                binding.savePetButton.setText(R.string.create_persona);
                
                if (!isPet) {
                    Toast.makeText(AddPetActivity.this, "Pet not clearly detected. Ensure the photo is clear.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing()) return;
                binding.savePetButton.setEnabled(true);
                binding.savePetButton.setText(R.string.create_persona);
                Log.e(TAG, "Validation error", e);
            }
        });
    }

    private void uploadToCloudinary() {
        try {
            File compressedFile = ImageUtils.compressImage(this, selectedImageUri, "pet_" + System.currentTimeMillis() + ".jpg");
            MediaManager.get().upload(compressedFile.getAbsolutePath())
                    .unsigned("ml_defaults")
                    .callback(new UploadCallback() {
                        @Override public void onStart(String requestId) { }
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) { }
                        @Override public void onSuccess(String requestId, Map resultData) {
                            if (isFinishing()) return;
                            String imageUrl = (String) resultData.get("secure_url");
                            savePetPersona(imageUrl);
                            if (compressedFile.exists()) compressedFile.delete();
                        }
                        @Override public void onError(String requestId, ErrorInfo error) {
                            if (isFinishing()) return;
                            setLoading(false);
                            Toast.makeText(AddPetActivity.this, "Upload Failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                            if (compressedFile.exists()) compressedFile.delete();
                        }
                        @Override public void onReschedule(String requestId, ErrorInfo error) {
                            if (compressedFile.exists()) compressedFile.delete();
                        }
                    }).dispatch();
        } catch (IOException e) {
            setLoading(false);
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void savePetPersona(String imageUrl) {
        String name = binding.petNameInput.getText().toString().trim();
        String breed = binding.breedInput.getText().toString().trim();
        String ageStr = binding.ageInput.getText().toString().trim();
        String weightStr = binding.weightInput.getText().toString().trim();
        
        int age = 0;
        double weight = 0.0;
        try {
            if (!ageStr.isEmpty()) age = Integer.parseInt(ageStr);
            if (!weightStr.isEmpty()) weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Number format error", e);
        }
        
        String size = "Medium";
        int checkedId = binding.sizeGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioSmall) size = "Small";
        else if (checkedId == R.id.radioLarge) size = "Large";

        List<String> selectedVibes = new ArrayList<>();
        for (int i = 0; i < binding.vibeChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) binding.vibeChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedVibes.add(chip.getText().toString());
            }
        }

        PetProfile profile = new PetProfile(name, breed, age, size);
        profile.setOwnerId(mAuth.getUid());
        profile.setOwnerName(UserManager.getInstance().getName());
        profile.setLocation(detectedLocation);
        profile.setWeight(weight);
        profile.setImageUrl(imageUrl);
        profile.setVibeTags(selectedVibes);
        profile.setAfraidOfLargeBreeds(binding.afraidLargeSwitch.isChecked());
        profile.setRequiresVaccinated(binding.requireVaccinatedSwitch.isChecked());
        profile.setVaccinated(binding.isVaccinatedSwitch.isChecked());

        db.collection("pet_profiles").add(profile).addOnSuccessListener(doc -> {
            if (isFinishing()) return;
            Toast.makeText(this, "Pet Persona Created!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            if (isFinishing()) return;
            setLoading(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
