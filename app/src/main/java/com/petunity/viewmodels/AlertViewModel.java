package com.petunity.viewmodels;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.models.Post;
import com.petunity.utils.ImageUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AlertViewModel extends AndroidViewModel {
    private final MutableLiveData<Boolean> isUploading = new MutableLiveData<>(false);
    private final MutableLiveData<String> uploadError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> alertSent = new MutableLiveData<>(false);
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public AlertViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Boolean> getIsUploading() { return isUploading; }
    public LiveData<String> getUploadError() { return uploadError; }
    public LiveData<Boolean> getAlertSent() { return alertSent; }

    public void sendAlert(Uri imageUri, String description, String animalType, String location, boolean isUrgent, String currentUserName, String currentUserProfileImageUrl) {
        if (imageUri == null) {
            uploadError.setValue("Please select an image");
            return;
        }

        isUploading.setValue(true);
        
        try {
            // Compress image before uploading to avoid null InputStream issues and save bandwidth
            File compressedFile = ImageUtils.compressImage(getApplication(), imageUri, "alert_" + System.currentTimeMillis() + ".jpg");
            
            MediaManager.get().upload(compressedFile.getAbsolutePath())
                    .unsigned("ml_defaults")
                    .callback(new UploadCallback() {
                        @Override public void onStart(String requestId) {}
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            saveToFirestore(imageUrl, description, animalType, location, isUrgent, currentUserName, currentUserProfileImageUrl);
                            if (compressedFile.exists()) compressedFile.delete();
                        }
                        @Override public void onError(String requestId, ErrorInfo error) {
                            isUploading.setValue(false);
                            uploadError.setValue("Upload failed: " + error.getDescription());
                            if (compressedFile.exists()) compressedFile.delete();
                        }
                        @Override public void onReschedule(String requestId, ErrorInfo error) {
                            if (compressedFile.exists()) compressedFile.delete();
                        }
                    }).dispatch();
        } catch (IOException e) {
            isUploading.setValue(false);
            uploadError.setValue("Failed to process image: " + e.getMessage());
        }
    }

    private void saveToFirestore(String imageUrl, String description, String animalType, String location, boolean isUrgent, String currentUserName, String currentUserProfileImageUrl) {
        String userId = FirebaseAuth.getInstance().getUid();
        
        Post newPost = new Post(currentUserName, "Just now · " + location, animalType + " Alert", description);
        newPost.setUserId(userId);
        newPost.setImageUrl(imageUrl);
        newPost.setUserProfileImageUrl(currentUserProfileImageUrl);
        newPost.setUrgent(isUrgent);
        newPost.setTimestamp(Timestamp.now());

        db.collection("posts").add(newPost)
                .addOnSuccessListener(docRef -> {
                    crossPostToLostAndFound(imageUrl, animalType, location, currentUserName, userId, docRef.getId());
                })
                .addOnFailureListener(e -> {
                    isUploading.setValue(false);
                    uploadError.setValue("Failed to save post: " + e.getMessage());
                });
    }

    private void crossPostToLostAndFound(String imageUrl, String type, String location, String userName, String userId, String postId) {
        Map<String, Object> pet = new HashMap<>();
        pet.put("name", "Stray " + type);
        pet.put("breed", type);
        pet.put("location", location);
        pet.put("status", "lost");
        pet.put("ownerName", userName);
        pet.put("userId", userId);
        pet.put("imageUrl", imageUrl);
        pet.put("linkedPostId", postId);
        pet.put("timestamp", Timestamp.now());

        db.collection("pet_listing").add(pet)
                .addOnSuccessListener(doc -> {
                    isUploading.setValue(false);
                    alertSent.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isUploading.setValue(false);
                    uploadError.setValue("Alert sent but cross-post failed.");
                });
    }
}
