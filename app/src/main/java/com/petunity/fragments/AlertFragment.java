package com.petunity.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.viewmodels.AlertViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class AlertFragment extends Fragment {
    private static final String TAG = "AlertFragment";
    private MaterialCardView photoCardView;
    private ImageView photoImageView;
    private View addPhotoLayout;
    private TextView locationText;
    private TextInputEditText descriptionInput;
    private Spinner animalTypeSpinner, conditionSpinner;
    private CheckBox urgentAlertCheckBox;
    private MaterialButton sendButton;
    private View loadingOverlay;

    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserName = "User";
    private String currentUserProfileImageUrl = null;
    private FusedLocationProviderClient fusedLocationClient;
    private AlertViewModel viewModel;

    private final ActivityResultLauncher<String[]> getImage = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    loadPreviewImage(uri);
                }
            }
    );

    private void loadPreviewImage(Uri uri) {
        try {
            // Take persistable URI permission
            requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap != null) {
                photoImageView.setImageBitmap(bitmap);
                photoImageView.setVisibility(View.VISIBLE);
                addPhotoLayout.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load preview image", e);
            photoImageView.setImageURI(uri); // Fallback
            photoImageView.setVisibility(View.VISIBLE);
            addPhotoLayout.setVisibility(View.GONE);
        }
    }

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
        viewModel = new ViewModelProvider(this).get(AlertViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // UI Bindings
        photoCardView = view.findViewById(R.id.photoCardView);
        photoImageView = view.findViewById(R.id.photoImageView);
        addPhotoLayout = view.findViewById(R.id.addPhotoLayout);
        locationText = view.findViewById(R.id.locationText);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        animalTypeSpinner = view.findViewById(R.id.animalTypeSpinner);
        conditionSpinner = view.findViewById(R.id.conditionSpinner);
        urgentAlertCheckBox = view.findViewById(R.id.urgentAlertCheckBox);
        sendButton = view.findViewById(R.id.sendButton);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        fetchCurrentUserInfo();
        setupSpinners();
        setupClickListeners();
        observeViewModel();
        detectLocation();
    }

    private void observeViewModel() {
        viewModel.getIsUploading().observe(getViewLifecycleOwner(), isUploading -> {
            sendButton.setEnabled(!isUploading);
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(isUploading ? View.VISIBLE : View.GONE);
            }
            sendButton.setText(isUploading ? "Sending..." : "Send Alert");
        });

        viewModel.getUploadError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && isAdded()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getAlertSent().observe(getViewLifecycleOwner(), sent -> {
            if (sent && isAdded()) {
                Toast.makeText(getContext(), "Alert shared with the community!", Toast.LENGTH_SHORT).show();
                clearForm();
            }
        });
    }

    private void setupClickListeners() {
        photoCardView.setOnClickListener(v -> getImage.launch(new String[]{"image/*"}));
        locationText.setOnClickListener(v -> showLocationEditDialog());

        sendButton.setOnClickListener(v -> {
            String loc = locationText.getText().toString();
            String desc = descriptionInput.getText() != null ? descriptionInput.getText().toString() : "";
            String type = animalTypeSpinner.getSelectedItem() != null ? animalTypeSpinner.getSelectedItem().toString() : "Other";
            boolean urgent = urgentAlertCheckBox.isChecked();

            if (selectedImageUri == null) {
                Toast.makeText(getContext(), "Please add a photo of the pet", Toast.LENGTH_SHORT).show();
                return;
            }
            if (desc.trim().isEmpty()) {
                Toast.makeText(getContext(), "Please add a description", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.sendAlert(selectedImageUri, desc, type, loc, urgent, currentUserName, currentUserProfileImageUrl);
        });
    }

    private void detectLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null && isAdded()) {
                    updateLocationUI(location);
                }
            }).addOnFailureListener(e -> Log.e(TAG, "Failed to get location", e));
        }
    }

    private void updateLocationUI(Location location) {
        if (!isAdded()) return;
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty() && isAdded()) {
                String city = addresses.get(0).getLocality();
                String admin = addresses.get(0).getAdminArea();
                locationText.setText((city != null ? city : "Unknown") + ", " + (admin != null ? admin : ""));
            }
        } catch (IOException ignored) {}
    }

    private void fetchCurrentUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && isAdded()) {
                            currentUserName = doc.getString("name");
                            if (currentUserName == null) currentUserName = "User";
                            currentUserProfileImageUrl = doc.getString("profileImageUrl");
                        }
                    });
        }
    }

    private void setupSpinners() {
        String[] types = {"Dog", "Cat", "Bird", "Other"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        animalTypeSpinner.setAdapter(typeAdapter);
        
        String[] conditions = {"Healthy", "Injured", "Aggressive"};
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, conditions);
        conditionSpinner.setAdapter(conditionAdapter);
    }

    private void clearForm() {
        descriptionInput.setText("");
        urgentAlertCheckBox.setChecked(false);
        photoImageView.setVisibility(View.GONE);
        addPhotoLayout.setVisibility(View.VISIBLE);
        selectedImageUri = null;
    }

    private void showLocationEditDialog() {
        final EditText input = new EditText(requireContext());
        input.setText(locationText.getText());
        new AlertDialog.Builder(requireContext())
                .setTitle("Update Location")
                .setView(input)
                .setPositiveButton("Set", (d, w) -> {
                    String newLoc = input.getText().toString().trim();
                    if (!newLoc.isEmpty()) {
                        locationText.setText(newLoc);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
