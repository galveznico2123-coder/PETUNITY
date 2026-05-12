package com.petunity.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petunity.R;
import com.petunity.models.UserManager;

import java.util.HashMap;
import java.util.Map;

public class VerificationActivity extends AppCompatActivity {

    private String name, email, password, membershipType, generatedCode;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get data from Intent
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        email = intent.getStringExtra("email");
        password = intent.getStringExtra("password");
        membershipType = intent.getStringExtra("membershipType");
        generatedCode = intent.getStringExtra("generatedCode");

        TextView verificationSentText = findViewById(R.id.verificationSentText);
        verificationSentText.setText("Enter the 6-digit code sent to\n" + email);

        TextInputEditText verificationCodeInput = findViewById(R.id.verificationCodeInput);
        MaterialButton verifyButton = findViewById(R.id.verifyButton);
        TextView resendCodeText = findViewById(R.id.resendCodeText);

        verifyButton.setOnClickListener(v -> {
            String enteredCode = verificationCodeInput.getText().toString().trim();
            if (enteredCode.equals(generatedCode)) {
                createAccountInFirebase();
            } else {
                Toast.makeText(this, "Invalid verification code", Toast.LENGTH_SHORT).show();
            }
        });

        resendCodeText.setOnClickListener(v -> {
            Toast.makeText(this, "Resending code...", Toast.LENGTH_SHORT).show();
            finish(); 
        });
    }

    private void createAccountInFirebase() {
        MaterialButton verifyButton = findViewById(R.id.verifyButton);
        verifyButton.setEnabled(false);
        verifyButton.setText("Creating Account...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid());
                        }
                    } else {
                        verifyButton.setEnabled(true);
                        verifyButton.setText("Verify");
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(VerificationActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("membershipType", membershipType);
        user.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    completeSignUp();
                })
                .addOnFailureListener(e -> {
                    Log.e("PetUnity", "Error saving user to Firestore", e);
                    Toast.makeText(VerificationActivity.this, "Error saving profile", Toast.LENGTH_SHORT).show();
                    completeSignUp(); // Still move forward since Auth succeeded
                });
    }

    private void completeSignUp() {
        UserManager.getInstance().setName(name);
        UserManager.getInstance().setMembershipType(membershipType);

        Toast.makeText(this, "Account verified! Welcome, " + name, Toast.LENGTH_SHORT).show();
        
        Intent intent;
        if (UserManager.getInstance().isRescuer()) {
            intent = new Intent(this, RescuerMainActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
