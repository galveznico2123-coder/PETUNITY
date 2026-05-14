package com.petunity.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.databinding.ActivityMembershipBinding;
import com.petunity.databinding.DialogVerifyTaskBinding;
import com.petunity.databinding.ItemMembershipTaskBinding;
import com.petunity.models.UserManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MembershipActivity extends AppCompatActivity {
    private static final String TAG = "MembershipActivity";

    private ActivityMembershipBinding binding;
    private FirebaseFirestore db;
    private String userId;
    
    private Uri currentProofUri;

    private final ActivityResultLauncher<String[]> getProofImage = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    currentProofUri = uri;
                    // Note: We'll update the dialog UI if it's currently showing.
                    // Since the launcher is in the activity, we'd need a reference to the dialog's binding.
                    // A better way is to handle the picker inside the dialog logic or use a ViewModel.
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMembershipBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.my_membership));
        }

        updateUI();
        setupTasks();
    }

    private void updateUI() {
        UserManager user = UserManager.getInstance();
        double helped = user.getPetsHelped();
        String level = user.getMembershipLevelName();

        binding.membershipLevelText.setText(level);
        binding.petsHelpedCount.setText(String.format(Locale.getDefault(), "%.1f Pets Helped", helped));

        int currentPoints = (int) Math.ceil(helped);
        int nextThreshold = 0;
        String nextRank = "";

        if (currentPoints <= 5) { nextThreshold = 6; nextRank = "Pet Buddy 🐶"; }
        else if (currentPoints <= 15) { nextThreshold = 16; nextRank = "Pet Guardian 🛡️"; }
        else if (currentPoints <= 30) { nextThreshold = 31; nextRank = "Pet Champion 🏆"; }
        else if (currentPoints <= 50) { nextThreshold = 51; nextRank = "Pet Hero 🦸‍♀️🦸"; }

        if (nextThreshold > 0) {
            int progress = (int) ((helped / nextThreshold) * 100);
            binding.membershipProgress.setProgress(progress);
            binding.nextLevelText.setText(String.format(Locale.getDefault(), "%d more pets to reach %s", (nextThreshold - currentPoints), nextRank));
        } else {
            binding.membershipProgress.setProgress(100);
            binding.nextLevelText.setText("Status: Verified Pet Hero! 🦸");
        }
    }

    private void setupTasks() {
        setupTask(binding.taskShare, "Share PetUnity", "+0.1", "Share", 0.1, "Screenshot Link/ID", false);
        setupTask(binding.taskMeal, "Donate 1 meal", "+1.0", "Donate", 1.0, "Transaction Ref No.", true);
        setupTask(binding.taskVaccine, "Donate 1 vaccine", "+1.0", "Donate", 1.0, "Clinic Receipt ID", true);
        setupTask(binding.taskSpay, "Sponsor spay/neuter", "+1.0", "Sponsor", 1.0, "Cert Code", true);
        setupTask(binding.taskAdopt, "Adopt a pet", "+1.0", "Adopt", 1.0, "Contract No.", true);
        setupTask(binding.taskVolunteer, "Volunteer (1hr)", "+0.5", "Book", 0.5, "Shelter Approval Code", true);
    }

    private void setupTask(ItemMembershipTaskBinding taskBinding, String title, String points, String action, double reward, String validationHint, boolean reqPhoto) {
        taskBinding.taskTitle.setText(title);
        taskBinding.taskPoints.setText(points + " pets helped");
        taskBinding.taskActionButton.setText(action);
        taskBinding.taskActionButton.setOnClickListener(v -> {
            if (title.contains("Share")) startShareIntent();
            showValidationDialog(title, reward, validationHint, reqPhoto);
        });
    }

    private void startShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Join PetUnity and help save stray animals! #PetUnityApp");
        startActivity(Intent.createChooser(intent, "Share PetUnity"));
    }

    private void showValidationDialog(String title, double reward, String hint, boolean requiresPhoto) {
        currentProofUri = null;
        DialogVerifyTaskBinding dialogBinding = DialogVerifyTaskBinding.inflate(getLayoutInflater());
        
        dialogBinding.verifyTitle.setText("Verify: " + title);
        dialogBinding.verifyInput.setHint(hint);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Submit Proof", null) // Set listener later to control dismissal
                .setNegativeButton("Cancel", null)
                .create();

        dialogBinding.proofPhotoCard.setOnClickListener(v -> {
            // This is a bit tricky with ActivityResultLauncher because we need to update dialogBinding.
            // For now, we'll use a listener or just rely on the user adding photo before clicking submit.
            // Simplified for this refactor.
            getProofImage.launch(new String[]{"image/*"});
            
            // Periodically check if uri is set (or use a better pattern like a ViewModel + State)
            dialogBinding.getRoot().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentProofUri != null) {
                        dialogBinding.proofImageView.setImageURI(currentProofUri);
                        dialogBinding.proofImageView.setVisibility(View.VISIBLE);
                        dialogBinding.addProofPhotoLayout.setVisibility(View.GONE);
                    } else if (dialog.isShowing()) {
                        dialogBinding.getRoot().postDelayed(this, 500);
                    }
                }
            }, 500);
        });

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String code = dialogBinding.verifyInput.getText().toString().trim();
                if (code.isEmpty() && currentProofUri == null) {
                    Toast.makeText(this, "Proof ID or Photo is required!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (requiresPhoto && currentProofUri == null) {
                    Toast.makeText(this, "A photo receipt is required for this task", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (currentProofUri != null) {
                    uploadProof(title, code, reward, currentProofUri);
                } else {
                    submitToFirestore(title, code, reward, null);
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void uploadProof(String title, String code, double reward, Uri uri) {
        binding.uploadProgressBar.setVisibility(View.VISIBLE);
        MediaManager.get().upload(uri).unsigned("ml_defaults").callback(new UploadCallback() {
            @Override public void onSuccess(String id, Map data) {
                runOnUiThread(() -> submitToFirestore(title, code, reward, (String) data.get("secure_url")));
            }
            @Override public void onError(String id, ErrorInfo e) {
                runOnUiThread(() -> {
                    binding.uploadProgressBar.setVisibility(View.GONE);
                    Toast.makeText(MembershipActivity.this, "Upload failed: " + e.getDescription(), Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onStart(String id) {}
            @Override public void onProgress(String id, long b, long t) {}
            @Override public void onReschedule(String id, ErrorInfo e) {}
        }).dispatch();
    }

    private void submitToFirestore(String task, String proof, double reward, @Nullable String imgUrl) {
        if (userId == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("userName", UserManager.getInstance().getName());
        data.put("task", task);
        data.put("proofCode", proof);
        if (imgUrl != null) data.put("evidenceUrl", imgUrl);
        data.put("timestamp", System.currentTimeMillis());
        data.put("status", "pending_review");

        db.collection("task_verifications").add(data).addOnSuccessListener(doc -> {
            binding.uploadProgressBar.setVisibility(View.GONE);
            updateUserPoints(reward);
        }).addOnFailureListener(e -> {
            binding.uploadProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error submitting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUserPoints(double reward) {
        UserManager user = UserManager.getInstance();
        double newTotal = user.getPetsHelped() + reward;
        db.collection("users").document(userId).update("petsHelped", newTotal).addOnSuccessListener(aVoid -> {
            user.setPetsHelped(newTotal);
            updateUI();
            Toast.makeText(this, "Proof submitted! Rank updated.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
