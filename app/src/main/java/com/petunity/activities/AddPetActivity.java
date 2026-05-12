package com.petunity.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RadioGroup;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.models.ImageValidator;
import com.petunity.models.PetProfile;
import com.petunity.models.UserManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AddPetActivity extends AppCompatActivity {
    private static final String TAG = "AddPetActivity";
    private TextInputEditText petNameInput, breedInput, ageInput, weightInput;
    private ImageView petImageView;
    private MaterialButton savePetButton;
    private ChipGroup vibeChipGroup;
    private RadioGroup sizeGroup;
    private SwitchMaterial afraidLargeSwitch, requireVaccinatedSwitch, isVaccinatedSwitch;
    
    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    private String detectedLocation = "Nearby";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        petImageView.setImageBitmap(selectedBitmap);
                        validateImage();
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image", e);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Identity Setup
        petNameInput = findViewById(R.id.petNameInput);
        breedInput = findViewById(R.id.breedInput);
        ageInput = findViewById(R.id.ageInput);
        weightInput = findViewById(R.id.weightInput);
        petImageView = findViewById(R.id.petImageView);
        sizeGroup = findViewById(R.id.sizeGroup);
        
        // Vibe Tags
        vibeChipGroup = findViewById(R.id.vibeChipGroup);
        
        // Dealbreakers
        afraidLargeSwitch = findViewById(R.id.afraidLargeSwitch);
        requireVaccinatedSwitch = findViewById(R.id.requireVaccinatedSwitch);
        isVaccinatedSwitch = findViewById(R.id.isVaccinatedSwitch);
        
        savePetButton = findViewById(R.id.savePetButton);
        MaterialButton selectImageButton = findViewById(R.id.selectImageButton);

        detectLocation();

        if (selectImageButton != null) {
            selectImageButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            });
        }

        if (savePetButton != null) {
            savePetButton.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() == null) {
                    Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedImageUri == null) {
                    Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadToCloudinary();
            });
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
                        detectedLocation = "Nearby";
                    }
                }
            });
        }
    }

    private void validateImage() {
        if (savePetButton == null) return;
        savePetButton.setEnabled(false);
        savePetButton.setText(R.string.validating);

        ImageValidator.validateIsPet(this, selectedBitmap, new ImageValidator.ValidationCallback() {
            @Override
            public void onResult(boolean isPet) {
                if (isPet) {
                    savePetButton.setEnabled(true);
                    savePetButton.setText(R.string.create_persona);
                } else {
                    savePetButton.setEnabled(false);
                    savePetButton.setText(R.string.no_pet_detected);
                    Toast.makeText(AddPetActivity.this, "No pet detected. Please use a clear photo.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                savePetButton.setEnabled(true);
                savePetButton.setText(R.string.create_persona);
                Log.e(TAG, "Validation error", e);
            }
        });
    }

    private void uploadToCloudinary() {
        if (selectedImageUri == null || savePetButton == null) return;
        savePetButton.setEnabled(false);
        savePetButton.setText(R.string.finalizing_persona);

        MediaManager.get().upload(selectedImageUri)
                .unsigned("ml_defaults")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) { }
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) { }
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        savePetPersona(imageUrl);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        savePetButton.setEnabled(true);
                        savePetButton.setText(R.string.create_persona);
                        Toast.makeText(AddPetActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) { }
                }).dispatch();
    }

    private void savePetPersona(String imageUrl) {
        String name = petNameInput.getText() != null ? petNameInput.getText().toString().trim() : "";
        String breed = breedInput.getText() != null ? breedInput.getText().toString().trim() : "";
        String ageStr = ageInput.getText() != null ? ageInput.getText().toString().trim() : "";
        String weightStr = weightInput.getText() != null ? weightInput.getText().toString().trim() : "";
        
        int age = ageStr.isEmpty() ? 0 : Integer.parseInt(ageStr);
        double weight = weightStr.isEmpty() ? 0.0 : Double.parseDouble(weightStr);
        
        String size = "Medium";
        int checkedId = sizeGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radioSmall) size = "Small";
        else if (checkedId == R.id.radioLarge) size = "Large";

        List<String> selectedVibes = new ArrayList<>();
        for (int i = 0; i < vibeChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) vibeChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedVibes.add(chip.getText().toString());
            }
        }

        PetProfile profile = new PetProfile(name, breed, age, size);
        profile.setOwnerId(mAuth.getUid());
        profile.setOwnerName(UserManager.getInstance().getName()); // Set Owner Name
        profile.setLocation(detectedLocation); // Set detected location
        profile.setWeight(weight);
        profile.setImageUrl(imageUrl);
        profile.setVibeTags(selectedVibes);
        profile.setAfraidOfLargeBreeds(afraidLargeSwitch.isChecked());
        profile.setRequiresVaccinated(requireVaccinatedSwitch.isChecked());
        profile.setVaccinated(isVaccinatedSwitch.isChecked());

        db.collection("pet_profiles").add(profile).addOnSuccessListener(doc -> {
            Toast.makeText(this, "Pet Persona Created!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            savePetButton.setEnabled(true);
        });
    }
}
