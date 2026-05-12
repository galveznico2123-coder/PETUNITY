package com.petunity.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.Arrays;
import java.util.List;

public class ImageValidator {
    private static final String TAG = "ImageValidator";
    
    // Keywords we allow for Pet posts - made more specific
    private static final List<String> ALLOWED_LABELS = Arrays.asList(
            "Dog", "Cat", "Puppy", "Kitten", "Bird", "Hamster", "Rabbit", "Canidae", "Felidae", "Parrot"
    );

    public interface ValidationCallback {
        void onResult(boolean isPet);
        void onError(Exception e);
    }

    public static void validateIsPet(Context context, Bitmap bitmap, ValidationCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        // Use a slightly higher confidence threshold for better accuracy
        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build();
        ImageLabeler labeler = ImageLabeling.getClient(options);

        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    boolean foundPet = false;
                    for (ImageLabel label : labels) {
                        String text = label.getText();
                        float confidence = label.getConfidence();
                        Log.d(TAG, "Detected: " + text + " (" + confidence + ")");
                        
                        if (isAllowed(text)) {
                            foundPet = true;
                            break;
                        }
                    }
                    callback.onResult(foundPet);
                })
                .addOnFailureListener(callback::onError);
    }

    private static boolean isAllowed(String label) {
        for (String allowed : ALLOWED_LABELS) {
            if (label.toLowerCase().contains(allowed.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
