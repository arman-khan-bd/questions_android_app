package com.questions.govt.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.questions.govt.R;
import com.questions.govt.data.local.AppDatabase;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.model.User;
import com.questions.govt.data.repository.UserRepository;
import com.questions.govt.ui.auth.LoginActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified User Settings & Privacy Manager:
 * 1. Personal & Academic Data Manager (Instant JSON API sync)
 * 2. Privacy Settings (Hide Phone & Hide Email toggles)
 * 3. Secure Password Changer with previous password verification
 * 4. Sign Out & Session Controls
 */
public class UserSettingsActivity extends AppCompatActivity {

    private MaterialToolbar toolbarSettings;

    // Section 1: User Data
    private TextInputLayout tilSettingsName, tilSettingsPhone, tilSettingsTargetExam, tilSettingsInstitution, tilSettingsPassingYear, tilSettingsBio;
    private TextInputEditText etSettingsName, etSettingsPhone, etSettingsTargetExam, etSettingsInstitution, etSettingsPassingYear, etSettingsBio;
    private MaterialButton btnSaveUserData, btnOpenFullEditor;

    // Section 2: Privacy
    private SwitchMaterial switchPhonePrivate, switchEmailPrivate;

    // Section 3: Password Changer
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnChangePassword;

    // Section 4: Account Actions
    private MaterialButton btnLogout;
    private ProgressBar progressBarSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        initViews();
        populateUserData();
        setupListeners();
    }

    private void initViews() {
        toolbarSettings = findViewById(R.id.toolbarSettings);
        toolbarSettings.setNavigationOnClickListener(v -> finish());

        tilSettingsName = findViewById(R.id.tilSettingsName);
        etSettingsName = findViewById(R.id.etSettingsName);
        tilSettingsPhone = findViewById(R.id.tilSettingsPhone);
        etSettingsPhone = findViewById(R.id.etSettingsPhone);
        tilSettingsTargetExam = findViewById(R.id.tilSettingsTargetExam);
        etSettingsTargetExam = findViewById(R.id.etSettingsTargetExam);
        tilSettingsInstitution = findViewById(R.id.tilSettingsInstitution);
        etSettingsInstitution = findViewById(R.id.etSettingsInstitution);
        tilSettingsPassingYear = findViewById(R.id.tilSettingsPassingYear);
        etSettingsPassingYear = findViewById(R.id.etSettingsPassingYear);
        tilSettingsBio = findViewById(R.id.tilSettingsBio);
        etSettingsBio = findViewById(R.id.etSettingsBio);

        btnSaveUserData = findViewById(R.id.btnSaveUserData);
        btnOpenFullEditor = findViewById(R.id.btnOpenFullEditor);

        switchPhonePrivate = findViewById(R.id.switchPhonePrivate);
        switchEmailPrivate = findViewById(R.id.switchEmailPrivate);

        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnLogout = findViewById(R.id.btnLogout);
        progressBarSettings = findViewById(R.id.progressBarSettings);
    }

    private void populateUserData() {
        User user = SessionManager.getInstance().getUser();
        if (user == null) return;

        if (user.getName() != null) etSettingsName.setText(user.getName());
        if (user.getPhone() != null) etSettingsPhone.setText(user.getPhone());
        if (user.getTargetExam() != null) etSettingsTargetExam.setText(user.getTargetExam());
        if (user.getUniversity() != null) etSettingsInstitution.setText(user.getUniversity());
        if (user.getGraduationYear() != null) etSettingsPassingYear.setText(user.getGraduationYear());
        if (user.getBio() != null) etSettingsBio.setText(user.getBio());

        switchPhonePrivate.setChecked(user.isPhonePrivate());
        switchEmailPrivate.setChecked(user.isEmailPrivate());
    }

    private void setupListeners() {
        btnSaveUserData.setOnClickListener(v -> saveUserData());

        btnOpenFullEditor.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            startActivity(intent);
        });

        switchPhonePrivate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                syncPrivacyPreferences();
            }
        });

        switchEmailPrivate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                syncPrivacyPreferences();
            }
        });

        btnChangePassword.setOnClickListener(v -> changePassword());

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void saveUserData() {
        String name = etSettingsName.getText() != null ? etSettingsName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            tilSettingsName.setError("Full name cannot be blank");
            return;
        }
        tilSettingsName.setError(null);

        String phone = etSettingsPhone.getText() != null ? etSettingsPhone.getText().toString().trim() : "";
        String targetExam = etSettingsTargetExam.getText() != null ? etSettingsTargetExam.getText().toString().trim() : "";
        String institution = etSettingsInstitution.getText() != null ? etSettingsInstitution.getText().toString().trim() : "";
        String passingYear = etSettingsPassingYear.getText() != null ? etSettingsPassingYear.getText().toString().trim() : "";
        String bio = etSettingsBio.getText() != null ? etSettingsBio.getText().toString().trim() : "";

        setLoading(true);

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        if (!TextUtils.isEmpty(phone)) {
            fields.put("phone", phone);
        }
        fields.put("target_exam", targetExam);
        fields.put("university", institution);
        fields.put("education_level", passingYear);
        fields.put("graduation_year", passingYear);
        fields.put("bio", bio);
        fields.put("is_phone_private", switchPhonePrivate.isChecked());
        fields.put("is_email_private", switchEmailPrivate.isChecked());

        UserRepository.getInstance(this).updateProfileJson(fields, new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(UserSettingsActivity.this, "Profile information saved!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(UserSettingsActivity.this, message != null ? message : "Failed to save profile info.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncPrivacyPreferences() {
        boolean phonePrivate = switchPhonePrivate.isChecked();
        boolean emailPrivate = switchEmailPrivate.isChecked();

        UserRepository.getInstance(this).updatePrivacy(phonePrivate, emailPrivate, new UserRepository.UserUpdateCallback() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(UserSettingsActivity.this, "Privacy settings updated.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(UserSettingsActivity.this, message != null ? message : "Privacy update failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void changePassword() {
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(currentPassword)) {
            tilCurrentPassword.setError("Please enter your current (previous) password");
            return;
        }

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            tilNewPassword.setError("New password must be at least 6 characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("New passwords do not match");
            return;
        }

        setLoading(true);

        UserRepository.getInstance(this).changePassword(currentPassword, newPassword, confirmPassword, new UserRepository.PasswordChangeCallback() {
            @Override
            public void onSuccess(String message) {
                setLoading(false);
                Toast.makeText(UserSettingsActivity.this, message, Toast.LENGTH_LONG).show();
                etCurrentPassword.setText("");
                etNewPassword.setText("");
                etConfirmPassword.setText("");
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                tilCurrentPassword.setError(message);
                Toast.makeText(UserSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of your account?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    SessionManager.getInstance().clearSession();
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getInstance(this).clearAllTables();
                    });
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setLoading(boolean isLoading) {
        progressBarSettings.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveUserData.setEnabled(!isLoading);
        btnChangePassword.setEnabled(!isLoading);
    }
}
