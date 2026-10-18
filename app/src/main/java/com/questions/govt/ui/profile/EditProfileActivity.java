package com.questions.govt.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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
 * - Uploads multi-part WebP encoded assets to backend API
 */
public class EditProfileActivity extends AppCompatActivity {

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

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);

        toolbarEditProfile.setNavigationOnClickListener(v -> finish());

        btnPickAvatar.setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        btnPickCover.setOnClickListener(v -> coverPickerLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
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

        setLoading(true);

        Map<String, RequestBody> fields = new HashMap<>();
        fields.put("name", createPartFromString(name));
        if (!phone.isEmpty()) fields.put("phone", createPartFromString(phone));
        if (!bio.isEmpty()) fields.put("bio", createPartFromString(bio));
        if (!institution.isEmpty()) fields.put("university", createPartFromString(institution));
        if (!degree.isEmpty()) fields.put("target_exam", createPartFromString(degree));
        if (!passingYear.isEmpty()) fields.put("education_level", createPartFromString(passingYear));

        MultipartBody.Part avatarPart = prepareFilePart("avatar", selectedAvatarUri);
        MultipartBody.Part coverPart = prepareFilePart("cover_photo", selectedCoverUri);

        UserRepository.getInstance(this).updateProfile(fields, avatarPart, coverPart, new UserRepository.UserUpdateCallback() {
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
    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        if (fileUri == null) return null;

        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream != null) {
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] bytes = byteBuffer.toByteArray();
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), bytes);
                return MultipartBody.Part.createFormData(partName, partName + ".jpg", requestFile);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private RequestBody createPartFromString(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    private void setLoading(boolean isLoading) {
        btnSaveProfile.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
