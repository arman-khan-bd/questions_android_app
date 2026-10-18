package com.govtexam.prep.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.govtexam.prep.R;
import com.govtexam.prep.data.local.SessionManager;
import com.govtexam.prep.data.remote.ApiClient;
import com.govtexam.prep.data.remote.request.LoginRequest;
import com.govtexam.prep.data.remote.response.ApiResponse;
import com.govtexam.prep.data.remote.response.AuthData;
import com.govtexam.prep.ui.main.MainActivity;
import com.govtexam.prep.util.FeatureFlagManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity for Sanctum Bearer token authentication.
 * Handles instant sign-in and device FCM token synchronization.
 */
public class LoginActivity extends AppCompatActivity {

    private ImageView ivLoginLogo;
    private TextView tvLoginTitle;
    private TextInputEditText etIdentifier;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private ProgressBar pbLogin;
    private TextView tvRegisterPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        applyDynamicBrandingAndFlags();
    }

    private void initViews() {
        ivLoginLogo = findViewById(R.id.ivLoginLogo);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        pbLogin = findViewById(R.id.pbLogin);
        tvRegisterPrompt = findViewById(R.id.tvRegisterPrompt);

        btnLogin.setOnClickListener(v -> performLogin());

        tvRegisterPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void applyDynamicBrandingAndFlags() {
        FeatureFlagManager flagManager = FeatureFlagManager.getInstance();

        // Remote Feature Flag: Hide registration entry point if disabled by Admin
        boolean registrationEnabled = flagManager.isUserRegistrationEnabled();
        tvRegisterPrompt.setVisibility(registrationEnabled ? View.VISIBLE : View.GONE);

        // Dynamic Branding Logo
        String logoUrl = flagManager.getLogoUrl();
        if (!TextUtils.isEmpty(logoUrl)) {
            Glide.with(this)
                    .load(logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(ivLoginLogo);
        }
    }

    private void performLogin() {
        String identifier = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(identifier)) {
            etIdentifier.setError("Please enter your Phone or Email");
            etIdentifier.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        LoginRequest request = new LoginRequest(identifier, password);
        ApiClient.getApiService().login(request).enqueue(new Callback<ApiResponse<AuthData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthData>> call, Response<ApiResponse<AuthData>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AuthData authData = response.body().getData();

                    // Save token & user in SessionManager
                    SessionManager.getInstance().saveAuthToken(authData.getToken());
                    if (authData.getUser() != null) {
                        SessionManager.getInstance().saveUser(authData.getUser());
                    }

                    // Sync device FCM token
                    syncFcmToken();

                    Toast.makeText(LoginActivity.this, "Welcome back, " +
                            (authData.getUser() != null ? authData.getUser().getName() : "Student") + "!", Toast.LENGTH_SHORT).show();

                    // Navigate to MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = "Invalid credentials. Please verify your phone/email and password.";
                    if (response.body() != null && !TextUtils.isEmpty(response.body().getMessage())) {
                        errorMsg = response.body().getMessage();
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthData>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
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
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    // FCM token synced
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    // Sync will retry on next token refresh
                }
            });
        }
    }

    private void setLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        pbLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
