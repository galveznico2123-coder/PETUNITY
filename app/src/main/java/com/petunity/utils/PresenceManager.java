package com.petunity.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PresenceManager {
    public static void updateStatus(boolean online) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> status = new HashMap<>();
        status.put("online", online);
        status.put("lastSeen", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(status)
                .addOnFailureListener(e -> {
                    // If document doesn't exist, try set
                    if (e.getMessage() != null && e.getMessage().contains("NOT_FOUND")) {
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                .set(status, com.google.firebase.firestore.SetOptions.merge());
                    }
                });
    }
}
