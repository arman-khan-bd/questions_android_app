package com.questions.govt.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.textfield.TextInputEditText;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.request.RegisterRequest;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.AuthData;
import com.questions.govt.ui.main.MainActivity;
import com.questions.govt.util.FeatureFlagManager;
import com.questions.govt.util.NetworkErrorParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * RegisterActivity enabling instant account registration without OTP.
 * Provides a live preview of the backend-generated unique @username handle.
 */
public class RegisterActivity extends AppCompatActivity {

    private ImageView ivRegisterLogo;
    private TextInputEditText etName;
    private TextInputEditText etIdentifier;
    private TextInputEditText etPassword;
    private TextView tvUsernamePreview;
    private Button btnRegister;
    private ProgressBar pbRegister;
    private TextView tvLoginPrompt;

    private int previewSuffix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        previewSuffix = 1000 + new Random().nextInt(9000);

        initViews();
        applyDynamicBranding();
        setupUsernameLivePreview();
    }

    private void initViews() {
        ivRegisterLogo = findViewById(R.id.ivRegisterLogo);
        etName = findViewById(R.id.etName);
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        tvUsernamePreview = findViewById(R.id.tvUsernamePreview);
        btnRegister = findViewById(R.id.btnRegister);
        pbRegister = findViewById(R.id.pbRegister);
        tvLoginPrompt = findViewById(R.id.tvLoginPrompt);

        btnRegister.setOnClickListener(v -> performRegister());
        tvLoginPrompt.setOnClickListener(v -> finish());
    }

    private void applyDynamicBranding() {
        String logoUrl = FeatureFlagManager.getInstance().getLogoUrl();
        if (!TextUtils.isEmpty(logoUrl)) {
            Glide.with(this)
                    .load(logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(ivRegisterLogo);
        }
    }

    private void setupUsernameLivePreview() {
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                String input = s != null ? s.toString().trim() : "";
                if (input.isEmpty()) {
                    tvUsernamePreview.setText(getString(R.string.username_preview, "@student_" + previewSuffix));
                } else {
                    String cleanName = input.toLowerCase()
                            .replaceAll("[^a-zA-Z0-9]", "_")
                            .replaceAll("_+", "_");
                    if (cleanName.endsWith("_")) {
                        cleanName = cleanName.substring(0, cleanName.length() - 1);
                    }
                    tvUsernamePreview.setText(getString(R.string.username_preview, "@" + cleanName + "_" + previewSuffix));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performRegister() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String identifier = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("Please enter your full name");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(identifier)) {
            etIdentifier.setError("Please enter your Phone or Email");
            etIdentifier.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        RegisterRequest request = new RegisterRequest(name, identifier, password);
        ApiClient.getApiService().register(request).enqueue(new Callback<ApiResponse<AuthData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthData>> call, Response<ApiResponse<AuthData>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AuthData authData = response.body().getData();

                    // Store Laravel Sanctum Bearer token and User model
                    SessionManager.getInstance().saveAuthToken(authData.getToken());
                    if (authData.getUser() != null) {
                        SessionManager.getInstance().saveUser(authData.getUser());
                    }

                    // Sync device FCM token
                    syncFcmToken();

                    String username = authData.getUser() != null ? authData.getUser().getUsername() : "";
                    Toast.makeText(RegisterActivity.this,
                            "Account created successfully! Welcome " + username,
                            Toast.LENGTH_LONG).show();

                    // Navigate to MainActivity
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = NetworkErrorParser.parseError(response, "Registration failed. This phone or email may already be in use.");
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthData>> call, Throwable t) {
                setLoading(false);
                String errorMsg = NetworkErrorParser.parseFailure(t, getString(R.string.network_error));
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void syncFcmToken() {
        String fcmToken = SessionManager.getInstance().getFcmToken();
        if (!TextUtils.isEmpty(fcmToken)) {
            Map<String, String> body = new HashMap<>();
            body.put("fcm_token", fcmToken);
            ApiClient.getApiService().syncFcmToken(body).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
            });
        }
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        pbRegister.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
