package com.questions.govt.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Edit Student Profile Activity:
 * - Update personal bio, target exam, institution, phone
 * - Pick and preview new Avatar and 16:9 Cover Photo
 * - High-speed JSON fallback when no image attachments are selected
 * - Safe image downsampling and JPEG compression to eliminate payload errors
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
        if (user.getPhone() != null) etPhone.setText(user.getPhone());
        if (user.getBio() != null) etBio.setText(user.getBio());
        if (user.getUniversity() != null) etInstitution.setText(user.getUniversity());
        if (user.getTargetExam() != null) etDegree.setText(user.getTargetExam());
        if (user.getGraduationYear() != null) etPassingYear.setText(user.getGraduationYear());

        if (switchEditPhonePrivate != null) switchEditPhonePrivate.setChecked(user.isPhonePrivate());
        if (switchEditEmailPrivate != null) switchEditEmailPrivate.setChecked(user.isEmailPrivate());

        // Load existing images
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .placeholder(R.drawable.ic_app_logo)
                    .into(imgEditAvatar);
        }

        if (user.getCoverPhotoUrl() != null && !user.getCoverPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getCoverPhotoUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(R.drawable.bg_splash_gradient)
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

        MultipartBody.Part avatarPart = prepareFilePart("avatar", selectedAvatarUri);
        MultipartBody.Part coverPart = prepareFilePart("cover_photo", selectedCoverUri);

        UserRepository.UserUpdateCallback callback = new UserRepository.UserUpdateCallback() {
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
        };

        if (avatarPart == null && coverPart == null) {
            // Pure text update: Send clean JSON payload to avoid multipart boundaries and transport errors
            Map<String, Object> jsonFields = new HashMap<>();
            jsonFields.put("name", name);
            if (!TextUtils.isEmpty(phone)) {
                jsonFields.put("phone", phone);
            }
            jsonFields.put("bio", bio);
            jsonFields.put("university", institution);
            jsonFields.put("target_exam", degree);
            jsonFields.put("education_level", passingYear);
            jsonFields.put("graduation_year", passingYear);
            jsonFields.put("is_phone_private", isPhonePrivate);
            jsonFields.put("is_email_private", isEmailPrivate);

            UserRepository.getInstance(this).updateProfileJson(jsonFields, callback);
        } else {
            // Multipart payload with compressed image attachments
            Map<String, RequestBody> fields = new HashMap<>();
            fields.put("name", createPartFromString(name));
            fields.put("phone", createPartFromString(phone));
            fields.put("bio", createPartFromString(bio));
            fields.put("university", createPartFromString(institution));
            fields.put("target_exam", createPartFromString(degree));
            fields.put("education_level", createPartFromString(passingYear));
            fields.put("graduation_year", createPartFromString(passingYear));
            fields.put("is_phone_private", createPartFromString(String.valueOf(isPhonePrivate)));
            fields.put("is_email_private", createPartFromString(String.valueOf(isEmailPrivate)));

            UserRepository.getInstance(this).updateProfile(fields, avatarPart, coverPart, callback);
        }
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        if (fileUri == null) return null;

        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            if (bitmap == null) return null;

            // Safe downsample if dimensions exceed 1200px
            int maxDimension = 1200;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxDimension || height > maxDimension) {
                float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
                width = Math.round(ratio * width);
                height = Math.round(ratio * height);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream);
            byte[] bytes = outputStream.toByteArray();
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), bytes);
            return MultipartBody.Part.createFormData(partName, partName + ".jpg", requestFile);
        } catch (Exception e) {
            Log.e(TAG, "prepareFilePart error: " + e.getMessage());
        }
        return null;
    }

    private RequestBody createPartFromString(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value != null ? value : "");
    }

    private void setLoading(boolean isLoading) {
        btnSaveProfile.setEnabled(!isLoading);
        btnSaveProfile.setText(isLoading ? "" : getString(R.string.save_profile));
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
