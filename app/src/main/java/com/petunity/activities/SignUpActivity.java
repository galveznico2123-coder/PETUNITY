package com.petunity.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.petunity.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, passwordInput;
    private RadioGroup membershipGroup;
    private MaterialButton signupButton;
    private ProgressBar progressBar;

    private static final String SERVICE_ID = "service_szlhjp7";
    private static final String TEMPLATE_ID = "template_9uobggf";
    private static final String PUBLIC_KEY = "D0jQbO7kNY8s7OlRX";
    private static final String PRIVATE_KEY = "sv1gQ3LG6aoL3sBZLIood";

    interface EmailJsService {
        @POST("email/send")
        Call<Void> sendEmail(@Body Map<String, Object> data);
    }

    private EmailJsService emailJsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        membershipGroup = findViewById(R.id.membershipGroup);
        signupButton = findViewById(R.id.signupButton);
        progressBar = findViewById(R.id.signupProgressBar);
        TextView loginText = findViewById(R.id.loginText);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.emailjs.com/api/v1.0/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        emailJsService = retrofit.create(EmailJsService.class);

        signupButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String name = nameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            setLoading(true);
            sendVerificationCode(email, name, password);
        });

        loginText.setOnClickListener(v -> finish());
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            signupButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void sendVerificationCode(String email, String name, String password) {
        String generatedCode = String.format(Locale.US, "%06d", new Random().nextInt(1000000));
        Log.d("PetUnity", "Generated Code: " + generatedCode);

        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("to_email", email);
        templateParams.put("name", name);
        templateParams.put("verification code", generatedCode);
        templateParams.put("time", new SimpleDateFormat("hh:mm a", Locale.US).format(new Date()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("service_id", SERVICE_ID);
        payload.put("template_id", TEMPLATE_ID);
        payload.put("user_id", PUBLIC_KEY);
        payload.put("accessToken", PRIVATE_KEY);
        payload.put("template_params", templateParams);

        emailJsService.sendEmail(payload).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);

                if (response.isSuccessful()) {
                    String membershipType = "Citizen Member";
                    int checkedId = membershipGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.radioRescue) membershipType = "Rescuer Member";
                    else if (checkedId == R.id.radioPet) membershipType = "Pet Owner";

                    Intent intent = new Intent(SignUpActivity.this, VerificationActivity.class);
                    intent.putExtra("name", name);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    intent.putExtra("membershipType", membershipType);
                    intent.putExtra("generatedCode", generatedCode);
                    startActivity(intent);
                } else {
                    String error = "Error";
                    try { if (response.errorBody() != null) error = response.errorBody().string(); } catch (IOException e) {}
                    Log.e("PetUnity", "EmailJS error: " + error);
                    Toast.makeText(SignUpActivity.this, "Failed to send code.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Log.e("PetUnity", "Network failure", t);
                Toast.makeText(SignUpActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}