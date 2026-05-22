package com.petunity.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.petunity.R;
import com.petunity.databinding.ActivityLoginBinding;
import com.petunity.models.UserManager;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    public static final String PREFS_NAME = "PetUnityPrefs";
    public static final String PREF_REMEMBER_ME = "remember_me";
    public static final String PREF_AUTO_LOGIN = "auto_login";
    public static final String PREF_EMAIL = "saved_email";
    
    private static final String TYPE_GOOGLE_ID_TOKEN_CREDENTIAL = "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(this);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        checkAutoLogin();
        loadRememberedUser();
        requestNotificationPermission();
        setupClickListeners();
    }

    private void checkAutoLogin() {
        if (prefs.getBoolean(PREF_AUTO_LOGIN, false) && mAuth.getCurrentUser() != null) {
            fetchUserDataAndNavigate(mAuth.getCurrentUser());
        }
    }

    private void loadRememberedUser() {
        boolean isRemembered = prefs.getBoolean(PREF_REMEMBER_ME, false);
        binding.rememberMeCheckbox.setChecked(isRemembered);
        if (isRemembered) {
            String savedEmail = prefs.getString(PREF_EMAIL, "");
            binding.emailInput.setText(savedEmail);
        }
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> performEmailLogin());
        binding.signUpText.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
        );
        binding.googleButton.setOnClickListener(v -> performGoogleLogin());
        
        binding.forgotPasswordText.setOnClickListener(v -> {
            String email = binding.emailInput.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void performEmailLogin() {
        String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
        String password = binding.passwordInput.getText() != null ? binding.passwordInput.getText().toString().trim() : "";

        if (email.isEmpty()) {
            binding.emailInput.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            binding.passwordInput.setError("Password is required");
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
        binding.loginButton.setEnabled(!loading);
        binding.googleButton.setEnabled(!loading);
        binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
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
                        savePreferences(email);
                        fetchUserDataAndNavigate(mAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void savePreferences(String email) {
        SharedPreferences.Editor editor = prefs.edit();
        boolean rememberMe = binding.rememberMeCheckbox.isChecked();
        editor.putBoolean(PREF_REMEMBER_ME, rememberMe);
        editor.putBoolean(PREF_AUTO_LOGIN, rememberMe);
        if (rememberMe) {
            editor.putString(PREF_EMAIL, email);
        } else {
            editor.remove(PREF_EMAIL);
        }
        editor.apply();
    }

    private void fetchUserDataAndNavigate(FirebaseUser user) {
        if (user == null) {
            setLoading(false);
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        populateUserManager(doc);
                        updateFcmToken(user.getUid());
                        
                        Boolean profileCompleted = doc.getBoolean("profileCompleted");
                        if (Boolean.TRUE.equals(profileCompleted)) {
                            updateUI(user);
                        } else {
                            startActivity(new Intent(this, CompleteProfileActivity.class));
                            finish();
                        }
                    } else {
                        updateUI(user);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    updateUI(user);
                });
    }

    private void populateUserManager(DocumentSnapshot doc) {
        UserManager um = UserManager.getInstance();
        um.setName(doc.getString("name"));
        um.setEmail(doc.getString("email"));
        um.setPhone(doc.getString("phone"));
        um.setLocation(doc.getString("location"));
        um.setBio(doc.getString("bio"));
        um.setProfileImageUrl(doc.getString("profileImageUrl"));
        um.setMembershipType(doc.getString("membershipType") != null ? doc.getString("membershipType") : "Citizen Member");
        Double helped = doc.getDouble("petsHelped");
        if (helped != null) um.setPetsHelped(helped);
    }

    private void updateFcmToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null) {
                        db.collection("users").document(userId).update("fcmToken", token);
                    }
                    FirebaseMessaging.getInstance().subscribeToTopic("alerts");
                });
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
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
            } catch (Exception e) {
                setLoading(false);
                Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            }
        } else {
            setLoading(false);
            Toast.makeText(this, "Unsupported login method", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        prefs.edit().putBoolean(PREF_AUTO_LOGIN, true).apply();
                        checkIfUserExistsAndNavigate(mAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkIfUserExistsAndNavigate(FirebaseUser user) {
        if (user == null) {
            setLoading(false);
            return;
        }

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", user.getEmail());
                        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
                        userData.put("membershipType", "Citizen Member");
                        userData.put("createdAt", System.currentTimeMillis());
                        userData.put("profileCompleted", false);

                        db.collection("users").document(user.getUid()).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    UserManager.getInstance().setName(user.getDisplayName());
                                    updateFcmToken(user.getUid());
                                    startActivity(new Intent(this, CompleteProfileActivity.class));
                                    finish();
                                });
                    } else {
                        populateUserManager(doc);
                        updateFcmToken(user.getUid());
                        
                        Boolean profileCompleted = doc.getBoolean("profileCompleted");
                        if (Boolean.TRUE.equals(profileCompleted)) {
                            updateUI(user);
                        } else {
                            startActivity(new Intent(this, CompleteProfileActivity.class));
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    updateUI(user);
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(this, UserManager.getInstance().isRescuer() ? RescuerMainActivity.class : MainActivity.class);
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
