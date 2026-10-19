package com.questions.govt.ui.auth;

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
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.request.LoginRequest;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.AuthData;
import com.questions.govt.ui.main.MainActivity;
import com.questions.govt.util.AccountStatusDialogHelper;
import com.questions.govt.util.FeatureFlagManager;
import com.questions.govt.util.NetworkErrorParser;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity for Sanctum Bearer token authentication.
 * Handles instant sign-in, account suspension dialogs, and device FCM token synchronization.
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
        checkSuspensionExtra(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkSuspensionExtra(intent);
    }

    private void checkSuspensionExtra(Intent intent) {
        if (intent != null && "suspended".equalsIgnoreCase(intent.getStringExtra("extra_status"))) {
            String reason = intent.getStringExtra("extra_reason");
            String expires = intent.getStringExtra("extra_expires");
            AccountStatusDialogHelper.showAccountSuspended(this, reason, expires);
        }
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
                } else if (response.code() == 403) {
                    String reason = "Administrative action / community guidelines.";
                    String date = "Indefinitely";
                    boolean isSuspended = false;

                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(errorJson).getAsJsonObject();

                            if ((obj.has("is_suspended") && obj.get("is_suspended").getAsBoolean())
                                    || (obj.has("account_status") && "suspended".equalsIgnoreCase(obj.get("account_status").getAsString()))
                                    || (obj.has("message") && obj.get("message").getAsString().toLowerCase().contains("suspended"))) {
                                isSuspended = true;
                            }

                            if (obj.has("reason") && !obj.get("reason").isJsonNull()) {
                                reason = obj.get("reason").getAsString();
                            } else if (obj.has("suspension_reason") && !obj.get("suspension_reason").isJsonNull()) {
                                reason = obj.get("suspension_reason").getAsString();
                            }

                            if (obj.has("suspended_until") && !obj.get("suspended_until").isJsonNull()) {
                                date = obj.get("suspended_until").getAsString();
                            } else if (obj.has("suspension_expires_at") && !obj.get("suspension_expires_at").isJsonNull()) {
                                date = obj.get("suspension_expires_at").getAsString();
                            }
                        }
                    } catch (Exception ignored) {}

                    if (isSuspended) {
                        AccountStatusDialogHelper.showAccountSuspended(LoginActivity.this, reason, date);
                    } else {
                        String errorMsg = NetworkErrorParser.parseError(response, "Account access restricted. Please contact support.");
                        AccountStatusDialogHelper.showAccountDisabled(LoginActivity.this, errorMsg);
                    }
                } else {
                    String errorMsg = NetworkErrorParser.parseError(response, "Invalid credentials. Please verify your phone/email and password.");
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthData>> call, Throwable t) {
                setLoading(false);
                String errorMsg = NetworkErrorParser.parseFailure(t, getString(R.string.network_error));
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
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
        btnLogin.setText(isLoading ? "" : getString(R.string.login_button));
        pbLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
