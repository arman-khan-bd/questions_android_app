package com.questions.govt.ui.subject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.questions.govt.R;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.SubjectDetailsResponse;
import com.questions.govt.data.repository.TaxonomyRepository;
import com.questions.govt.ui.search.SearchResultActivity;
import com.questions.govt.util.NetworkErrorParser;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dedicated Subject Explorer Page:
 * Displays topics under the subject, related exam categories, trending tags,
 * and total question counts with instant filter navigation.
 */
public class SubjectDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_ID = "extra_subject_id";
    public static final String EXTRA_SUBJECT_NAME = "extra_subject_name";
    public static final String EXTRA_SUBJECT_COLOR = "extra_subject_color";

    private Toolbar toolbarSubject;
    private SwipeRefreshLayout swipeRefreshSubject;
    private ImageView ivSubjectCover;
    private TextView tvSubjectHeaderTitle;
    private TextView tvSubjectHeaderSubtitle;
    private TextView tvSubjectHeaderQuestionsCount;
    private MaterialButton btnExploreAllSubjectQuestions;
    private ProgressBar pbSubjectLoading;

    private LinearLayout layoutTopicsSection;
    private TextView tvTopicsHeader;
    private ChipGroup cgSubjectTopics;

    private LinearLayout layoutCategoriesSection;
    private TextView tvCategoriesHeader;
    private ChipGroup cgSubjectCategories;

    private LinearLayout layoutTagsSection;
    private TextView tvTagsHeader;
    private ChipGroup cgSubjectTags;

    private long subjectId = -1;
    private String subjectName = "";
    private String subjectColor = "#1E3A8A";
    private TaxonomyRepository taxonomyRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_detail);

        taxonomyRepository = TaxonomyRepository.getInstance(this);

        extractIntent();
        initViews();
        loadSubjectDetails();
    }

    private void extractIntent() {
        subjectId = getIntent().getLongExtra(EXTRA_SUBJECT_ID, -1);
        if (getIntent().hasExtra(EXTRA_SUBJECT_NAME)) {
            subjectName = getIntent().getStringExtra(EXTRA_SUBJECT_NAME);
        }
        if (getIntent().hasExtra(EXTRA_SUBJECT_COLOR)) {
            subjectColor = getIntent().getStringExtra(EXTRA_SUBJECT_COLOR);
        }
    }

    private void initViews() {
        toolbarSubject = findViewById(R.id.toolbarSubject);
        swipeRefreshSubject = findViewById(R.id.swipeRefreshSubject);
        ivSubjectCover = findViewById(R.id.ivSubjectCover);
        tvSubjectHeaderTitle = findViewById(R.id.tvSubjectHeaderTitle);
        tvSubjectHeaderSubtitle = findViewById(R.id.tvSubjectHeaderSubtitle);
        tvSubjectHeaderQuestionsCount = findViewById(R.id.tvSubjectHeaderQuestionsCount);
        btnExploreAllSubjectQuestions = findViewById(R.id.btnExploreAllSubjectQuestions);
        pbSubjectLoading = findViewById(R.id.pbSubjectLoading);

        layoutTopicsSection = findViewById(R.id.layoutTopicsSection);
        tvTopicsHeader = findViewById(R.id.tvTopicsHeader);
        cgSubjectTopics = findViewById(R.id.cgSubjectTopics);

        layoutCategoriesSection = findViewById(R.id.layoutCategoriesSection);
        tvCategoriesHeader = findViewById(R.id.tvCategoriesHeader);
        cgSubjectCategories = findViewById(R.id.cgSubjectCategories);

        layoutTagsSection = findViewById(R.id.layoutTagsSection);
        tvTagsHeader = findViewById(R.id.tvTagsHeader);
        cgSubjectTags = findViewById(R.id.cgSubjectTags);

        setSupportActionBar(toolbarSubject);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(!TextUtils.isEmpty(subjectName) ? subjectName : "Subject Explorer");
        }
        toolbarSubject.setNavigationOnClickListener(v -> finish());

        if (!TextUtils.isEmpty(subjectName)) {
            tvSubjectHeaderTitle.setText(subjectName);
        }

        btnExploreAllSubjectQuestions.setOnClickListener(v -> {
            Intent intent = new Intent(SubjectDetailActivity.this, SearchResultActivity.class);
            intent.putExtra(SearchResultActivity.EXTRA_SUBJECT_ID, subjectId);
            intent.putExtra(SearchResultActivity.EXTRA_TITLE, subjectName);
            startActivity(intent);
        });

        swipeRefreshSubject.setOnRefreshListener(this::loadSubjectDetails);
    }

    private void loadSubjectDetails() {
        if (subjectId <= 0) {
            swipeRefreshSubject.setRefreshing(false);
            Toast.makeText(this, "Invalid subject ID", Toast.LENGTH_SHORT).show();
            return;
        }

        pbSubjectLoading.setVisibility(View.VISIBLE);

        ApiClient.getApiService().getSubjectDetails(subjectId).enqueue(new Callback<ApiResponse<SubjectDetailsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SubjectDetailsResponse>> call, Response<ApiResponse<SubjectDetailsResponse>> response) {
                pbSubjectLoading.setVisibility(View.GONE);
                swipeRefreshSubject.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    bindSubjectData(response.body().getData());
                } else {
                    fallbackToOfflineData();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SubjectDetailsResponse>> call, Throwable t) {
                pbSubjectLoading.setVisibility(View.GONE);
                swipeRefreshSubject.setRefreshing(false);
                fallbackToOfflineData();
            }
        });
    }

    private void bindSubjectData(SubjectDetailsResponse data) {
        TaxonomyItem sub = data.getSubject();
        if (sub != null) {
            subjectName = sub.getTitle();
            tvSubjectHeaderTitle.setText(subjectName);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(subjectName);
            }
            int qCount = sub.getQuestionsCount();
            tvSubjectHeaderQuestionsCount.setText(qCount > 0 ? qCount + " Questions" : "MCQs Available");

            if (!TextUtils.isEmpty(sub.getThumbnailUrl())) {
                Glide.with(this)
                        .load(sub.getThumbnailUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.bg_splash_gradient)
                        .error(R.drawable.bg_splash_gradient)
                        .into(ivSubjectCover);
            }
        }

        // 1. Populate Topics
        List<TaxonomyItem> topics = data.getTopics();
        cgSubjectTopics.removeAllViews();
        if (topics != null && !topics.isEmpty()) {
            layoutTopicsSection.setVisibility(View.VISIBLE);
            tvTopicsHeader.setText("Topics (" + topics.size() + ")");
            for (TaxonomyItem topic : topics) {
                String label = topic.getTitle();
                if (topic.getQuestionsCount() > 0) {
                    label += " (" + topic.getQuestionsCount() + ")";
                }
                Chip chip = createPillChip(label, R.color.surface_variant, R.color.primary);
                chip.setOnClickListener(v -> {
                    Intent intent = new Intent(SubjectDetailActivity.this, SearchResultActivity.class);
                    intent.putExtra(SearchResultActivity.EXTRA_TOPIC_ID, topic.getId());
                    intent.putExtra(SearchResultActivity.EXTRA_TITLE, topic.getTitle());
                    startActivity(intent);
                });
                cgSubjectTopics.addView(chip);
            }
        } else {
            layoutTopicsSection.setVisibility(View.GONE);
        }

        // 2. Populate Categories
        List<TaxonomyItem> categories = data.getCategories();
        cgSubjectCategories.removeAllViews();
        if (categories != null && !categories.isEmpty()) {
            layoutCategoriesSection.setVisibility(View.VISIBLE);
            tvCategoriesHeader.setText("Exam Categories (" + categories.size() + ")");
            for (TaxonomyItem cat : categories) {
                String label = cat.getTitle();
                if (cat.getQuestionsCount() > 0) {
                    label += " (" + cat.getQuestionsCount() + ")";
                }
                Chip chip = createPillChip(label, R.color.surface_variant, R.color.secondary);
                chip.setOnClickListener(v -> {
                    Intent intent = new Intent(SubjectDetailActivity.this, SearchResultActivity.class);
                    intent.putExtra(SearchResultActivity.EXTRA_CATEGORY_ID, cat.getId());
                    intent.putExtra(SearchResultActivity.EXTRA_TITLE, cat.getTitle());
                    startActivity(intent);
                });
                cgSubjectCategories.addView(chip);
            }
        } else {
            layoutCategoriesSection.setVisibility(View.GONE);
        }

        // 3. Populate Tags
        List<TaxonomyItem> tags = data.getTags();
        cgSubjectTags.removeAllViews();
        if (tags != null && !tags.isEmpty()) {
            layoutTagsSection.setVisibility(View.VISIBLE);
            tvTagsHeader.setText("Relevant Tags (" + tags.size() + ")");
            for (TaxonomyItem tag : tags) {
                String rawName = tag.getTitle() != null ? tag.getTitle().replaceFirst("^#", "").trim() : "";
                String label = "#" + rawName;
                if (tag.getQuestionsCount() > 0) {
                    label += " (" + tag.getQuestionsCount() + ")";
                }
                Chip chip = createPillChip(label, R.color.surface_variant, R.color.text_secondary);
                chip.setOnClickListener(v -> {
                    Intent intent = new Intent(SubjectDetailActivity.this, SearchResultActivity.class);
                    intent.putExtra(SearchResultActivity.EXTRA_TAG, rawName);
                    intent.putExtra(SearchResultActivity.EXTRA_TITLE, "#" + rawName);
                    startActivity(intent);
                });
                cgSubjectTags.addView(chip);
            }
        } else {
            layoutTagsSection.setVisibility(View.GONE);
        }
    }

    private void fallbackToOfflineData() {
        // Load topics from Room cache
        taxonomyRepository.getTopics().observe(this, allTopics -> {
            if (allTopics != null && !allTopics.isEmpty()) {
                cgSubjectTopics.removeAllViews();
                int matchCount = 0;
                for (TaxonomyItem t : allTopics) {
                    if (t.getParentId() == subjectId || subjectId <= 0) {
                        matchCount++;
                        Chip chip = createPillChip(t.getTitle(), R.color.surface_variant, R.color.primary);
                        chip.setOnClickListener(v -> {
                            Intent intent = new Intent(SubjectDetailActivity.this, SearchResultActivity.class);
                            intent.putExtra(SearchResultActivity.EXTRA_TOPIC_ID, t.getId());
                            intent.putExtra(SearchResultActivity.EXTRA_TITLE, t.getTitle());
                            startActivity(intent);
                        });
                        cgSubjectTopics.addView(chip);
                    }
                }
                if (matchCount > 0) {
                    layoutTopicsSection.setVisibility(View.VISIBLE);
                    tvTopicsHeader.setText("Topics (" + matchCount + ")");
                }
            }
        });
    }

    private Chip createPillChip(String text, int bgRes, int textColorRes) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setChipCornerRadius(24f);
        chip.setChipBackgroundColorResource(bgRes);
        chip.setTextColor(ContextCompat.getColor(this, textColorRes));
        chip.setChipStrokeColorResource(R.color.border);
        chip.setChipStrokeWidth(1.5f);
        chip.setTextSize(12f);
        return chip;
    }
}
