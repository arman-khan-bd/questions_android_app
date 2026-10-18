package com.questions.govt.ui.question;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.questions.govt.R;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.repository.TaxonomyRepository;
import com.questions.govt.util.FeatureFlagManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Community MCQ Question Creation Activity.
 * Supports image attachment (converted to .webp by backend), 4 options, explanation,
 * taxonomy selection, and respects the auto_publish_user_questions flag.
 */
public class CreateQuestionActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvPublishStatusBanner;
    private TextInputEditText etQuestionText;
    private Button btnAttachImage;
    private ImageView ivQuestionPreview;
    private ImageView btnRemoveImage;

    private TextInputEditText etOptionA;
    private TextInputEditText etOptionB;
    private TextInputEditText etOptionC;
    private TextInputEditText etOptionD;

    private ChipGroup cgCorrectOption;
    private TextInputEditText etExplanation;
    private ChipGroup cgQuestionDifficulty;

    private AutoCompleteTextView actvSubject;
    private AutoCompleteTextView actvCategory;
    private TextInputEditText etTagsInput;

    private Button btnSubmitQuestion;
    private ProgressBar pbSubmitQuestion;

    private Uri selectedImageUri = null;
    private final List<TaxonomyItem> subjectList = new ArrayList<>();
    private final List<TaxonomyItem> categoryList = new ArrayList<>();

    private long selectedSubjectId = -1;
    private long selectedCategoryId = -1;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivQuestionPreview.setVisibility(View.VISIBLE);
                    btnRemoveImage.setVisibility(View.VISIBLE);
                    Glide.with(this).load(uri).into(ivQuestionPreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_question);

        initViews();
        setupTaxonomyDropdowns();
        bindPublicationNotice();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvPublishStatusBanner = findViewById(R.id.tvPublishStatusBanner);
        etQuestionText = findViewById(R.id.etQuestionText);
        btnAttachImage = findViewById(R.id.btnAttachImage);
        ivQuestionPreview = findViewById(R.id.ivQuestionPreview);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        etOptionA = findViewById(R.id.etOptionA);
        etOptionB = findViewById(R.id.etOptionB);
        etOptionC = findViewById(R.id.etOptionC);
        etOptionD = findViewById(R.id.etOptionD);

        cgCorrectOption = findViewById(R.id.cgCorrectOption);
        etExplanation = findViewById(R.id.etExplanation);
        cgQuestionDifficulty = findViewById(R.id.cgQuestionDifficulty);

        actvSubject = findViewById(R.id.actvSubject);
        actvCategory = findViewById(R.id.actvCategory);
        etTagsInput = findViewById(R.id.etTagsInput);

        btnSubmitQuestion = findViewById(R.id.btnSubmitQuestion);
        pbSubmitQuestion = findViewById(R.id.pbSubmitQuestion);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnAttachImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            ivQuestionPreview.setVisibility(View.GONE);
            btnRemoveImage.setVisibility(View.GONE);
        });

        btnSubmitQuestion.setOnClickListener(v -> submitQuestion());
    }

    private void bindPublicationNotice() {
        boolean autoPublish = FeatureFlagManager.getInstance().isAutoPublishUserQuestions();
        if (autoPublish) {
            tvPublishStatusBanner.setText("Instant Publish: Your question will be published live immediately upon submission.");
            tvPublishStatusBanner.setBackgroundResource(R.drawable.bg_option_correct);
        } else {
            tvPublishStatusBanner.setText("Notice: Submitted questions will be held for Admin Verification before going live.");
            tvPublishStatusBanner.setBackgroundResource(R.drawable.bg_option_card);
        }
    }

    private void setupTaxonomyDropdowns() {
        TaxonomyRepository repository = TaxonomyRepository.getInstance(this);

        repository.getSubjects().observe(this, subjects -> {
            if (subjects != null && !subjects.isEmpty()) {
                subjectList.clear();
                subjectList.addAll(subjects);
                List<String> titles = new ArrayList<>();
                for (TaxonomyItem s : subjects) titles.add(s.getTitle());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, titles);
                actvSubject.setAdapter(adapter);
                actvSubject.setOnItemClickListener((parent, view, position, id) ->
                        selectedSubjectId = subjectList.get(position).getId());
            }
        });

        repository.getCategories().observe(this, categories -> {
            if (categories != null && !categories.isEmpty()) {
                categoryList.clear();
                categoryList.addAll(categories);
                List<String> titles = new ArrayList<>();
                for (TaxonomyItem c : categories) titles.add(c.getTitle());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, titles);
                actvCategory.setAdapter(adapter);
                actvCategory.setOnItemClickListener((parent, view, position, id) ->
                        selectedCategoryId = categoryList.get(position).getId());
            }
        });
    }

    private void submitQuestion() {
        String qText = etQuestionText.getText() != null ? etQuestionText.getText().toString().trim() : "";
        String optA = etOptionA.getText() != null ? etOptionA.getText().toString().trim() : "";
        String optB = etOptionB.getText() != null ? etOptionB.getText().toString().trim() : "";
        String optC = etOptionC.getText() != null ? etOptionC.getText().toString().trim() : "";
        String optD = etOptionD.getText() != null ? etOptionD.getText().toString().trim() : "";
        String explanation = etExplanation.getText() != null ? etExplanation.getText().toString().trim() : "";
        String tags = etTagsInput.getText() != null ? etTagsInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(qText)) {
            etQuestionText.setError("Question text is required");
            etQuestionText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(optA) || TextUtils.isEmpty(optB) || TextUtils.isEmpty(optC) || TextUtils.isEmpty(optD)) {
            Toast.makeText(this, "Please provide all 4 options (A, B, C, D)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(explanation)) {
            etExplanation.setError("Explanation is required");
            etExplanation.requestFocus();
            return;
        }

        if (selectedSubjectId <= 0) {
            Toast.makeText(this, "Please select a Subject", Toast.LENGTH_SHORT).show();
            return;
        }

        // Correct Option
        String correctOption = "A";
        int checkedOptId = cgCorrectOption.getCheckedChipId();
        if (checkedOptId == R.id.chipOptB) correctOption = "B";
        else if (checkedOptId == R.id.chipOptC) correctOption = "C";
        else if (checkedOptId == R.id.chipOptD) correctOption = "D";

        // Difficulty (Laravel validation requires: in:Easy,Medium,Hard)
        String difficulty = "Medium";
        int checkedDiffId = cgQuestionDifficulty.getCheckedChipId();
        if (checkedDiffId == R.id.chipDiffEasy) difficulty = "Easy";
        else if (checkedDiffId == R.id.chipDiffHard) difficulty = "Hard";

        setLoading(true);

        // Prepare multipart fields (matched to Laravel QuestionApiController::store)
        Map<String, RequestBody> fields = new HashMap<>();
        fields.put("title", createPartFromString(qText));
        fields.put("question_text", createPartFromString(qText));
        fields.put("option_a", createPartFromString(optA));
        fields.put("option_b", createPartFromString(optB));
        fields.put("option_c", createPartFromString(optC));
        fields.put("option_d", createPartFromString(optD));
        fields.put("correct_option", createPartFromString(correctOption));
        fields.put("explanation", createPartFromString(explanation));
        fields.put("difficulty", createPartFromString(difficulty));
        fields.put("subject_id", createPartFromString(String.valueOf(selectedSubjectId)));
        if (selectedCategoryId > 0) {
            fields.put("category_id", createPartFromString(String.valueOf(selectedCategoryId)));
        }
        if (!TextUtils.isEmpty(tags)) {
            fields.put("tags[]", createPartFromString(tags));
        }

        MultipartBody.Part imagePart = null;
        if (selectedImageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                if (inputStream != null) {
                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        byteBuffer.write(buffer, 0, len);
                    }
                    byte[] imageBytes = byteBuffer.toByteArray();
                    RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageBytes);
                    imagePart = MultipartBody.Part.createFormData("image", "question_image.jpg", requestFile);
                }
            } catch (Exception e) {
                // If image fails, proceed with text
            }
        }

        ApiClient.getApiService().createQuestion(fields, imagePart).enqueue(new Callback<ApiResponse<Question>>() {
            @Override
            public void onResponse(Call<ApiResponse<Question>> call, Response<ApiResponse<Question>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    boolean autoPublish = FeatureFlagManager.getInstance().isAutoPublishUserQuestions();
                    String msg = autoPublish ? "Question published successfully!" : "Question submitted for Admin Approval!";
                    Toast.makeText(CreateQuestionActivity.this, msg, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(CreateQuestionActivity.this, "Failed to submit question. Please verify fields.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Question>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreateQuestionActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RequestBody createPartFromString(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    private void setLoading(boolean isLoading) {
        btnSubmitQuestion.setEnabled(!isLoading);
        pbSubmitQuestion.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
