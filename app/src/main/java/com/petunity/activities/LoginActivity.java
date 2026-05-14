package com.petunity.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.petunity.R;
import com.petunity.databinding.ActivityLoginBinding;
import com.petunity.models.UserManager;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final String TYPE_GOOGLE_ID_TOKEN_CREDENTIAL = "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);

        requestNotificationPermission();

        // Set click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Email/Password login
        binding.loginButton.setOnClickListener(v -> performEmailLogin());

        // Sign up navigation
        binding.signUpText.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
        );

        // Google sign in
        binding.googleButton.setOnClickListener(v -> performGoogleLogin());
    }

    private void performEmailLogin() {
        String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
        String password = binding.passwordInput.getText() != null ? binding.passwordInput.getText().toString().trim() : "";

        if (email.isEmpty()) {
            binding.emailInput.setError("Email is required");
            binding.emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.passwordInput.setError("Password is required");
            binding.passwordInput.requestFocus();
            return;
        }

        setLoading(true);
        signInWithEmail(email, password);
    }

    private void performGoogleLogin() {
        setLoading(true);
        signInWithGoogle();
    }

    private void setLoading(boolean loading) {
        if (loading) {
            binding.loginButton.setEnabled(false);
            binding.googleButton.setEnabled(false);
            binding.loginButton.setText("");
            binding.loginProgressBar.setVisibility(View.VISIBLE);
        } else {
            binding.loginButton.setEnabled(true);
            binding.googleButton.setEnabled(true);
            binding.loginButton.setText(R.string.sign_in);
            binding.loginProgressBar.setVisibility(View.GONE);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        fetchUserDataAndNavigate(user);
                    } else {
                        setLoading(false);
                        String errorMessage = "Login failed: ";
                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Email login failed", task.getException());
                    }
                });
    }

    private void fetchUserDataAndNavigate(FirebaseUser user) {
        if (user == null) {
            setLoading(false);
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String membershipType = documentSnapshot.getString("membershipType");

                        if (name != null) UserManager.getInstance().setName(name);
                        if (membershipType != null) UserManager.getInstance().setMembershipType(membershipType);

                        updateFcmToken(user.getUid());
                    }
                    updateUI(user);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Error fetching user data", e);
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    updateUI(user);
                });
    }

    private void updateFcmToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("fcmToken", token);

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token updated successfully"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token", e));
                    }

                    // Subscribe to notification topics
                    FirebaseMessaging.getInstance().subscribeToTopic("alerts");
                    FirebaseMessaging.getInstance().subscribeToTopic("urgent_alerts");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get FCM token", e));
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(this, request, null, ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignIn(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        setLoading(false);
                        Log.e(TAG, "Google Sign In Error", e);
                        Toast.makeText(LoginActivity.this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleSignIn(Credential credential) {
        if (credential instanceof CustomCredential &&
                credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

            Bundle credentialData = ((CustomCredential) credential).getData();
            try {
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credentialData);
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
            } catch (Exception e) {
                setLoading(false);
                Log.e(TAG, "Error handling Google Sign In", e);
                Toast.makeText(LoginActivity.this, "Authentication error", Toast.LENGTH_SHORT).show();
            }
        } else {
            setLoading(false);
            Toast.makeText(LoginActivity.this, "Unsupported login method", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkIfUserExistsAndNavigate(user);
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Google Sign In Failed", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Google auth failed", task.getException());
                    }
                });
    }

    private void checkIfUserExistsAndNavigate(FirebaseUser user) {
        if (user == null) {
            setLoading(false);
            return;
        }

        String userId = user.getUid();
        String email = user.getEmail();
        String name = user.getDisplayName();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email);
                        userData.put("name", name != null ? name : "");
                        userData.put("membershipType", "standard");
                        userData.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    UserManager.getInstance().setName(name);
                                    UserManager.getInstance().setMembershipType("standard");
                                    updateFcmToken(userId);
                                    updateUI(user);
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    updateUI(user);
                                });
                    } else {
                        String existingName = documentSnapshot.getString("name");
                        String membershipType = documentSnapshot.getString("membershipType");

                        if (existingName != null) UserManager.getInstance().setName(existingName);
                        if (membershipType != null) UserManager.getInstance().setMembershipType(membershipType);

                        updateFcmToken(userId);
                        updateUI(user);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    updateUI(user);
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent;
            if (UserManager.getInstance().isRescuer()) {
                intent = new Intent(LoginActivity.this, RescuerMainActivity.class);
            } else {
                intent = new Intent(LoginActivity.this, MainActivity.class);
            }
            startActivity(intent);
            finish();
        } else {
            setLoading(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
