package com.petunity.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.models.Post;
import com.petunity.models.UserManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AlertFragment extends Fragment {
    private static final String TAG = "AlertFragment";
    private MaterialCardView photoCardView;
    private ImageView photoImageView;
    private View addPhotoLayout;
    private TextView locationText;
    private TextInputEditText descriptionInput;
    private Spinner animalTypeSpinner;
    private Spinner conditionSpinner;
    private CheckBox urgentAlertCheckBox;
    private MaterialButton sendButton;

    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserName = "User";
    private String currentUserProfileImageUrl = null;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String[]> getImage = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    photoImageView.setImageURI(uri);
                    photoImageView.setVisibility(View.VISIBLE);
                    addPhotoLayout.setVisibility(View.GONE);
                    
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {
                        Log.d(TAG, "Not a persistable URI");
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        photoCardView = view.findViewById(R.id.photoCardView);
        photoImageView = view.findViewById(R.id.photoImageView);
        addPhotoLayout = view.findViewById(R.id.addPhotoLayout);
        locationText = view.findViewById(R.id.locationText);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        animalTypeSpinner = view.findViewById(R.id.animalTypeSpinner);
        conditionSpinner = view.findViewById(R.id.conditionSpinner);
        urgentAlertCheckBox = view.findViewById(R.id.urgentAlertCheckBox);
        sendButton = view.findViewById(R.id.sendButton);

        fetchCurrentUserInfo();
        setupSpinners();
        setupClickListeners();
        detectLocation();
    }

    private void detectLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            
            locationText.setText("Detecting location...");
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    updateLocationUI(location);
                } else {
                    locationText.setText("Tap to set location");
                }
            });
        } else {
            locationText.setText("Tap to set location");
        }
    }

    private void updateLocationUI(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                if (city != null) {
                    locationText.setText(city + (state != null ? ", " + state : ""));
                } else {
                    locationText.setText("Unknown Location");
                }
            }
        } catch (IOException e) {
            locationText.setText("Coordinates: " + location.getLatitude() + ", " + location.getLongitude());
        }
    }

    private void fetchCurrentUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUserName = documentSnapshot.getString("name");
                            currentUserProfileImageUrl = documentSnapshot.getString("profileImageUrl");
                            UserManager.getInstance().setName(currentUserName);
                        }
                    });
        }
    }

    private void setupSpinners() {
        String[] animalTypes = {"Dog", "Cat", "Bird", "Rabbit", "Other"};
        String[] conditions = {"Healthy", "Injured", "Sick", "Aggressive"};

        ArrayAdapter<String> animalAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, animalTypes);
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, conditions);

        if (animalTypeSpinner != null) animalTypeSpinner.setAdapter(animalAdapter);
        if (conditionSpinner != null) conditionSpinner.setAdapter(conditionAdapter);
    }

    private void setupClickListeners() {
        if (photoCardView != null) photoCardView.setOnClickListener(v -> getImage.launch(new String[]{"image/*"}));
        if (locationText != null) locationText.setOnClickListener(v -> showLocationEditDialog());

        if (sendButton != null) {
            sendButton.setOnClickListener(v -> {
                String loc = locationText.getText().toString();
                if (loc.equals("Detecting location...") || loc.equals("Tap to set location") || loc.isEmpty()) {
                    Toast.makeText(requireContext(), "Please set a location", Toast.LENGTH_SHORT).show();
                    showLocationEditDialog();
                    return;
                }
                
                if (descriptionInput.getText().toString().isEmpty()) {
                    Toast.makeText(requireContext(), "Please add a description", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (selectedImageUri == null) {
                    Toast.makeText(requireContext(), "Please add a photo", Toast.LENGTH_SHORT).show();
                    return;
                }

                uploadToCloudinary();
            });
        }
    }

    private void uploadToCloudinary() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);
        sendButton.setText("Uploading...");

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
                        savePostToFirestore(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Cloudinary Error: " + error.getDescription());
                        sendButton.setEnabled(true);
                        sendButton.setText("Send Alert");
                        Toast.makeText(requireContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void savePostToFirestore(String imageUrl) {
        String description = descriptionInput.getText().toString();
        String animalType = animalTypeSpinner.getSelectedItem().toString();
        String location = locationText.getText().toString();
        String userId = mAuth.getUid();
        boolean isUrgent = urgentAlertCheckBox != null && urgentAlertCheckBox.isChecked();

        // 1. Post to Community Feed (posts collection)
        Post newPost = new Post(
                currentUserName,
                "Just now · " + location,
                animalType + " Alert",
                description
        );
        newPost.setUserId(userId);
        newPost.setImageUrl(imageUrl);
        newPost.setUserProfileImageUrl(currentUserProfileImageUrl); // Save avatar URL
        newPost.setSelf(true);
        newPost.setUrgent(isUrgent);

        db.collection("posts").add(newPost)
                .addOnSuccessListener(documentReference -> {
                    String postId = documentReference.getId();
                    crossPostToLostAndFound(animalType, location, imageUrl, postId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore Save Error: ", e);
                    sendButton.setEnabled(true);
                    sendButton.setText("Send Alert");
                    Toast.makeText(requireContext(), "Error saving alert", Toast.LENGTH_SHORT).show();
                });
    }

    private void crossPostToLostAndFound(String type, String location, String imageUrl, String postId) {
        Map<String, Object> pet = new HashMap<>();
        pet.put("name", "Stray " + type);
        pet.put("breed", type);
        pet.put("location", location);
        pet.put("timeAgo", "Just now");
        pet.put("status", "lost");
        pet.put("ownerName", currentUserName);
        pet.put("userId", mAuth.getUid());
        pet.put("ownerId", mAuth.getUid());
        pet.put("imageUrl", imageUrl);
        pet.put("linkedPostId", postId);
        
        db.collection("pet_listing").add(pet)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(requireContext(), "Alert sent and cross-posted!", Toast.LENGTH_LONG).show();
                    clearForm();
                    sendButton.setEnabled(true);
                    sendButton.setText("Send Alert");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Alert sent but cross-post failed", Toast.LENGTH_SHORT).show();
                    clearForm();
                    sendButton.setEnabled(true);
                    sendButton.setText("Send Alert");
                });
    }

    private void showLocationEditDialog() {
        EditText input = new EditText(requireContext());
        String currentLoc = locationText.getText().toString();
        if (!currentLoc.equals("Detecting location...") && !currentLoc.equals("Tap to set location")) {
            input.setText(currentLoc);
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Enter Location")
                .setMessage("Where is the pet located?")
                .setView(input)
                .setPositiveButton("Set", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) locationText.setText(val);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearForm() {
        if (descriptionInput != null) descriptionInput.setText("");
        if (urgentAlertCheckBox != null) urgentAlertCheckBox.setChecked(false);
        detectLocation();
        selectedImageUri = null;
        if (photoImageView != null) {
            photoImageView.setImageDrawable(null);
            photoImageView.setVisibility(View.GONE);
        }
        if (addPhotoLayout != null) addPhotoLayout.setVisibility(View.VISIBLE);
    }
}
