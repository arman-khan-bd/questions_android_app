package com.questions.govt.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.model.User;
import com.questions.govt.data.repository.UserRepository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Modern Student Profile Editor:
 * - Update personal details (full name, phone, bio)
 * - Academic aspirations (institution, target exam, passing year)
 * - Interactive Avatar and 16:9 Cover Photo pickers with safe background downscaling
 * - Ultra-fast, fail-safe JSON payload delivery with WebP encoding on backend
 * - Real-time Session & Room database synchronization
 */
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private MaterialToolbar toolbarEditProfile;
    private ImageView imgEditCover;
    private MaterialButton btnPickCover;
    private ImageView imgEditAvatar;
    private FloatingActionButton btnPickAvatar;

    private TextInputLayout tilName;
    private TextInputEditText etName;
    private TextInputLayout tilPhone;
    private TextInputEditText etPhone;
    private TextInputLayout tilBio;
    private TextInputEditText etBio;
    private TextInputLayout tilInstitution;
    private TextInputEditText etInstitution;
    private TextInputLayout tilDegree;
    private TextInputEditText etDegree;
    private TextInputLayout tilPassingYear;
    private TextInputEditText etPassingYear;

    private SwitchMaterial switchEditPhonePrivate;
    private SwitchMaterial switchEditEmailPrivate;
    private MaterialButton btnOpenSettingsFromEdit;

    private MaterialButton btnSaveProfile;
    private ProgressBar progressBar;

    private Uri selectedAvatarUri = null;
    private Uri selectedCoverUri = null;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> avatarPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedAvatarUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(imgEditAvatar);
                }
            });

    private final ActivityResultLauncher<String> coverPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedCoverUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(imgEditCover);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        populateUserData();
    }

    private void initViews() {
        toolbarEditProfile = findViewById(R.id.toolbarEditProfile);
        imgEditCover = findViewById(R.id.imgEditCover);
        btnPickCover = findViewById(R.id.btnPickCover);
        imgEditAvatar = findViewById(R.id.imgEditAvatar);
        btnPickAvatar = findViewById(R.id.btnPickAvatar);

        tilName = findViewById(R.id.tilName);
        etName = findViewById(R.id.etName);
        tilPhone = findViewById(R.id.tilPhone);
        etPhone = findViewById(R.id.etPhone);
        tilBio = findViewById(R.id.tilBio);
        etBio = findViewById(R.id.etBio);
        tilInstitution = findViewById(R.id.tilInstitution);
        etInstitution = findViewById(R.id.etInstitution);
        tilDegree = findViewById(R.id.tilDegree);
        etDegree = findViewById(R.id.etDegree);
        tilPassingYear = findViewById(R.id.tilPassingYear);
        etPassingYear = findViewById(R.id.etPassingYear);

        switchEditPhonePrivate = findViewById(R.id.switchEditPhonePrivate);
        switchEditEmailPrivate = findViewById(R.id.switchEditEmailPrivate);
        btnOpenSettingsFromEdit = findViewById(R.id.btnOpenSettingsFromEdit);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);

        toolbarEditProfile.setNavigationOnClickListener(v -> finish());

        btnPickAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        btnPickCover.setOnClickListener(v -> coverPickerLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());

        if (btnOpenSettingsFromEdit != null) {
            btnOpenSettingsFromEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserSettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void populateUserData() {
        User user = SessionManager.getInstance().getUser();
        if (user == null) return;

        if (user.getName() != null) etName.setText(user.getName());
        if (user.getPhone() != null) etPhone.setText(user.getPhone().trim());
        if (user.getBio() != null) etBio.setText(user.getBio());
        if (user.getUniversity() != null) etInstitution.setText(user.getUniversity());
        if (user.getTargetExam() != null) etDegree.setText(user.getTargetExam());
        if (user.getGraduationYear() != null) etPassingYear.setText(user.getGraduationYear());

        if (switchEditPhonePrivate != null) switchEditPhonePrivate.setChecked(user.isPhonePrivate());
        if (switchEditEmailPrivate != null) switchEditEmailPrivate.setChecked(user.isEmailPrivate());

        // Load existing avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(imgEditAvatar);
        }

        // Load existing cover photo
        if (user.getCoverPhotoUrl() != null && !user.getCoverPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getCoverPhotoUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(R.drawable.bg_splash_gradient)
                    .error(R.drawable.bg_splash_gradient)
                    .into(imgEditCover);
        }
    }

    private void saveProfileChanges() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            tilName.setError("Full name cannot be blank");
            return;
        }
        tilName.setError(null);

        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String bio = etBio.getText() != null ? etBio.getText().toString().trim() : "";
        String institution = etInstitution.getText() != null ? etInstitution.getText().toString().trim() : "";
        String degree = etDegree.getText() != null ? etDegree.getText().toString().trim() : "";
        String passingYear = etPassingYear.getText() != null ? etPassingYear.getText().toString().trim() : "";

        boolean isPhonePrivate = switchEditPhonePrivate != null && switchEditPhonePrivate.isChecked();
        boolean isEmailPrivate = switchEditEmailPrivate != null && switchEditEmailPrivate.isChecked();

        setLoading(true);

        // Process image encoding in background executor to ensure butter-smooth UI
        backgroundExecutor.execute(() -> {
            Map<String, Object> jsonFields = new HashMap<>();
            jsonFields.put("name", name);
            if (!TextUtils.isEmpty(phone)) {
                jsonFields.put("phone", phone);
            } else {
                jsonFields.put("phone", "");
            }
            jsonFields.put("bio", bio);
            jsonFields.put("university", institution);
            jsonFields.put("target_exam", degree);
            jsonFields.put("education_level", passingYear);
            jsonFields.put("graduation_year", passingYear);
            jsonFields.put("is_phone_private", isPhonePrivate);
            jsonFields.put("is_email_private", isEmailPrivate);

            if (selectedAvatarUri != null) {
                String avatarBase64 = convertUriToBase64DataUri(selectedAvatarUri, 600, 80);
                if (avatarBase64 != null) {
                    jsonFields.put("avatar_base64", avatarBase64);
                }
            }

            if (selectedCoverUri != null) {
                String coverBase64 = convertUriToBase64DataUri(selectedCoverUri, 1200, 80);
                if (coverBase64 != null) {
                    jsonFields.put("cover_photo_base64", coverBase64);
                }
            }

            runOnUiThread(() -> {
                UserRepository.getInstance(EditProfileActivity.this).updateProfileJson(jsonFields, new UserRepository.UserUpdateCallback() {
                    @Override
                    public void onSuccess(User updatedUser) {
                        setLoading(false);
                        Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        setLoading(false);
                        Toast.makeText(EditProfileActivity.this, message != null ? message : "Profile update failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    private String convertUriToBase64DataUri(Uri fileUri, int maxDimension, int quality) {
        if (fileUri == null) return null;
        try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
            if (inputStream == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return null;

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxDimension || height > maxDimension) {
                float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
                width = Math.round(ratio * width);
                height = Math.round(ratio * height);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            byte[] bytes = outputStream.toByteArray();
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + base64;
        } catch (Exception e) {
            Log.e(TAG, "convertUriToBase64DataUri error: " + e.getMessage(), e);
            return null;
        }
    }

    private void setLoading(boolean isLoading) {
        btnSaveProfile.setEnabled(!isLoading);
        btnSaveProfile.setText(isLoading ? "" : getString(R.string.save_profile));
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
    }
}
