package com.petunity.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.petunity.R;
import com.petunity.databinding.ActivitySignupBinding;

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
    private static final String TAG = "SignUpActivity";

    // EmailJS Credentials (Consider moving to a more secure location in production)
    private static final String SERVICE_ID = "service_szlhjp7";
    private static final String TEMPLATE_ID = "template_9uobggf";
    private static final String PUBLIC_KEY = "D0jQbO7kNY8s7OlRX";
    private static final String PRIVATE_KEY = "sv1gQ3LG6aoL3sBZLIood";

    private ActivitySignupBinding binding;

    public interface EmailJsService {
        @POST("email/send")
        Call<Void> sendEmail(@Body Map<String, Object> data);
    }

    private EmailJsService emailJsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeRetrofit();

        binding.signupButton.setOnClickListener(v -> {
            String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
            String name = binding.nameInput.getText() != null ? binding.nameInput.getText().toString().trim() : "";
            String password = binding.passwordInput.getText() != null ? binding.passwordInput.getText().toString().trim() : "";

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

        binding.loginText.setOnClickListener(v -> finish());
    }

    private void initializeRetrofit() {
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
    }

    private void setLoading(boolean loading) {
        binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.signupButton.setEnabled(!loading);
    }

    private void sendVerificationCode(String email, String name, String password) {
        String generatedCode = String.format(Locale.US, "%06d", new Random().nextInt(1000000));
        Log.d(TAG, "Generated Code: " + generatedCode);

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
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (isFinishing()) return;
                setLoading(false);

                if (response.isSuccessful()) {
                    String membershipType = "Citizen Member";
                    int checkedId = binding.membershipGroup.getCheckedRadioButtonId();
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
                    String error = "Unknown Error";
                    try { 
                        if (response.errorBody() != null) {
                            error = response.errorBody().string();
                        }
                    } catch (IOException ignored) {}
                    Log.e(TAG, "EmailJS error: " + error);
                    Toast.makeText(SignUpActivity.this, "Failed to send verification code.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (isFinishing()) return;
                setLoading(false);
                Log.e(TAG, "Network failure", t);
                Toast.makeText(SignUpActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
