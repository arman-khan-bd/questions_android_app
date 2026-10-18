package com.questions.govt.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.questions.govt.R;
import com.questions.govt.data.model.BannerItem;
import com.questions.govt.data.model.CustomExamRequest;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.remote.response.CustomExamGenerateResponse;
import com.questions.govt.data.remote.response.HomeFeedResponse;
import com.questions.govt.data.repository.ExamRepository;
import com.questions.govt.data.repository.TaxonomyRepository;
import com.questions.govt.ui.exam.ExamArenaActivity;
import com.questions.govt.ui.search.SearchResultActivity;
import com.questions.govt.util.FeatureFlagManager;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeFragment structured vertically:
 * 1. Hero Section with Auto-Scrolling ViewPager2 Carousel (.webp banners + deep links)
 * 2. Topics Horizontal RecyclerView with .webp icons
 * 3. All Subjects Grid RecyclerView with .webp thumbnails
 * 4. Categories & Tags interactive chip filters
 * 5. Inline Custom Exam Builder with taxonomy selection and dynamic negative marking
 */
public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private ViewPager2 vpHeroCarousel;
    private RecyclerView rvTopics;
    private RecyclerView rvSubjects;
    private TextView tvSubjectsCountBadge;
    private ChipGroup cgCategoriesAndTags;

    // Inline Custom Exam Builder
    private MaterialCardView cardCustomExamBuilder;
    private TextInputEditText etCustomExamTitle;
    private ChipGroup cgTaxonomyMultiSelect;
    private ChipGroup cgDifficultySelector;
    private ChipGroup cgNegativeMarkingPresets;
    private TextInputEditText etTotalQuestions;
    private TextInputEditText etExamDuration;
    private Button btnStartCustomExam;
    private ProgressBar pbExamGenerating;

    private BannerCarouselAdapter bannerAdapter;
    private TopicAdapter topicAdapter;
    private SubjectGridAdapter subjectAdapter;

    private TaxonomyRepository taxonomyRepository;
    private ExamRepository examRepository;

    private final Handler carouselHandler = new Handler(Looper.getMainLooper());
    private Runnable carouselRunnable;

    private final List<Long> selectedSubjectIds = new ArrayList<>();
    private final List<String> selectedTagNames = new ArrayList<>();
    private double selectedNegativeMarking = -0.25;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taxonomyRepository = TaxonomyRepository.getInstance(requireContext());
        examRepository = ExamRepository.getInstance(requireContext());

        initViews(view);
        setupAdapters();
        setupCustomExamBuilder();
        observeTaxonomyData();
        loadHomeFeedData();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        vpHeroCarousel = view.findViewById(R.id.vpHeroCarousel);
        rvTopics = view.findViewById(R.id.rvTopics);
        rvSubjects = view.findViewById(R.id.rvSubjects);
        tvSubjectsCountBadge = view.findViewById(R.id.tvSubjectsCountBadge);
        cgCategoriesAndTags = view.findViewById(R.id.cgCategoriesAndTags);

        cardCustomExamBuilder = view.findViewById(R.id.cardCustomExamBuilder);
        etCustomExamTitle = view.findViewById(R.id.etCustomExamTitle);
        cgTaxonomyMultiSelect = view.findViewById(R.id.cgTaxonomyMultiSelect);
        cgDifficultySelector = view.findViewById(R.id.cgDifficultySelector);
        cgNegativeMarkingPresets = view.findViewById(R.id.cgNegativeMarkingPresets);
        etTotalQuestions = view.findViewById(R.id.etTotalQuestions);
        etExamDuration = view.findViewById(R.id.etExamDuration);
        btnStartCustomExam = view.findViewById(R.id.btnStartCustomExam);
        pbExamGenerating = view.findViewById(R.id.pbExamGenerating);

        swipeRefresh.setOnRefreshListener(this::loadHomeFeedData);
    }

    private void setupAdapters() {
        // Hero Carousel
        bannerAdapter = new BannerCarouselAdapter(this::onBannerClicked);
        vpHeroCarousel.setAdapter(bannerAdapter);
        setupCarouselAutoScroll();

        // Topics (Horizontal)
        topicAdapter = new TopicAdapter(topic -> {
            Intent intent = new Intent(requireContext(), SearchResultActivity.class);
            intent.putExtra(SearchResultActivity.EXTRA_TOPIC_ID, topic.getId());
            intent.putExtra(SearchResultActivity.EXTRA_QUERY, topic.getTitle());
            startActivity(intent);
        });
        rvTopics.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTopics.setAdapter(topicAdapter);

        // Subjects (2-column Grid)
        subjectAdapter = new SubjectGridAdapter(subject -> {
            Intent intent = new Intent(requireContext(), SearchResultActivity.class);
            intent.putExtra(SearchResultActivity.EXTRA_SUBJECT_ID, subject.getId());
            intent.putExtra(SearchResultActivity.EXTRA_QUERY, subject.getTitle());
            startActivity(intent);
        });
        rvSubjects.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvSubjects.setAdapter(subjectAdapter);
    }

    private void setupCarouselAutoScroll() {
        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerAdapter != null && bannerAdapter.getItemCount() > 1 && vpHeroCarousel != null) {
                    int next = (vpHeroCarousel.getCurrentItem() + 1) % bannerAdapter.getItemCount();
                    vpHeroCarousel.setCurrentItem(next, true);
                    carouselHandler.postDelayed(this, 4000);
                }
            }
        };
    }

    private void onBannerClicked(BannerItem banner) {
        if (banner == null) return;
        String type = banner.getTargetType();
        long targetId = banner.getTargetId();

        if ("exam".equalsIgnoreCase(type) && targetId > 0) {
            Intent intent = new Intent(requireContext(), ExamArenaActivity.class);
            intent.putExtra(ExamArenaActivity.EXTRA_EXAM_ID, targetId);
            startActivity(intent);
        } else if ("subject".equalsIgnoreCase(type) && targetId > 0) {
            Intent intent = new Intent(requireContext(), SearchResultActivity.class);
            intent.putExtra(SearchResultActivity.EXTRA_SUBJECT_ID, targetId);
            startActivity(intent);
        } else if ("category".equalsIgnoreCase(type) && targetId > 0) {
            Intent intent = new Intent(requireContext(), SearchResultActivity.class);
            intent.putExtra(SearchResultActivity.EXTRA_CATEGORY_ID, targetId);
            startActivity(intent);
        } else if ("url".equalsIgnoreCase(type) && !TextUtils.isEmpty(banner.getTargetUrl())) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(banner.getTargetUrl()));
            startActivity(intent);
        }
    }

    private void observeTaxonomyData() {
        // Observe Subjects from local Room
        taxonomyRepository.getSubjects().observe(getViewLifecycleOwner(), subjects -> {
            if (subjects != null && !subjects.isEmpty()) {
                subjectAdapter.setSubjects(subjects);
                tvSubjectsCountBadge.setText(subjects.size() + " Subjects");
                populateTaxonomyMultiSelectChips(subjects, null);
            }
        });

        // Observe Topics from local Room
        taxonomyRepository.getTopics().observe(getViewLifecycleOwner(), topics -> {
            if (topics != null && !topics.isEmpty()) {
                topicAdapter.setTopics(topics);
            }
        });

        // Observe Categories & Tags from local Room
        taxonomyRepository.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                populateCategoryChips(categories);
            }
        });

        taxonomyRepository.getTags().observe(getViewLifecycleOwner(), tags -> {
            if (tags != null && !tags.isEmpty()) {
                populateTagChips(tags);
                populateTaxonomyMultiSelectChips(null, tags);
            }
        });
    }

    private void loadHomeFeedData() {
        swipeRefresh.setRefreshing(true);
        taxonomyRepository.fetchHomeFeed(new TaxonomyRepository.HomeFeedCallback() {
            @Override
            public void onSuccess(HomeFeedResponse response) {
                if (isAdded()) {
                    swipeRefresh.setRefreshing(false);
                    if (response != null && response.getBanners() != null) {
                        bannerAdapter.setBanners(response.getBanners());
                        carouselHandler.removeCallbacks(carouselRunnable);
                        carouselHandler.postDelayed(carouselRunnable, 4000);
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });
    }

    private void populateCategoryChips(List<TaxonomyItem> categories) {
        cgCategoriesAndTags.removeAllViews();
        for (TaxonomyItem cat : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(cat.getTitle());
            chip.setChipBackgroundColorResource(R.color.surface_variant);
            chip.setTextColor(getResources().getColor(R.color.primary));
            chip.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), SearchResultActivity.class);
                intent.putExtra(SearchResultActivity.EXTRA_CATEGORY_ID, cat.getId());
                intent.putExtra(SearchResultActivity.EXTRA_QUERY, cat.getTitle());
                startActivity(intent);
            });
            cgCategoriesAndTags.addView(chip);
        }
    }

    private void populateTagChips(List<TaxonomyItem> tags) {
        for (TaxonomyItem tag : tags) {
            Chip chip = new Chip(requireContext());
            chip.setText("#" + tag.getTitle());
            chip.setChipBackgroundColorResource(R.color.surface_variant);
            chip.setTextColor(getResources().getColor(R.color.text_secondary));
            chip.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), SearchResultActivity.class);
                intent.putExtra(SearchResultActivity.EXTRA_TAG, tag.getTitle());
                startActivity(intent);
            });
            cgCategoriesAndTags.addView(chip);
        }
    }

    private void setupCustomExamBuilder() {
        FeatureFlagManager flagManager = FeatureFlagManager.getInstance();

        // Remote Feature Flag: Hide custom exam builder if disabled by Admin
        boolean isBuilderEnabled = flagManager.isCustomExamBuilderEnabled();
        cardCustomExamBuilder.setVisibility(isBuilderEnabled ? View.VISIBLE : View.GONE);

        // Negative Marking Presets from Admin AppConfig
        cgNegativeMarkingPresets.removeAllViews();
        List<Double> presets = flagManager.getNegativeMarkingPresets();
        for (int i = 0; i < presets.size(); i++) {
            double preset = presets.get(i);
            Chip chip = new Chip(requireContext());
            chip.setText(String.format("%.2f", preset));
            chip.setCheckable(true);
            if (i == 1 || (presets.size() == 1)) {
                chip.setChecked(true);
                selectedNegativeMarking = preset;
            }
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    selectedNegativeMarking = preset;
                }
            });
            cgNegativeMarkingPresets.addView(chip);
        }

        btnStartCustomExam.setOnClickListener(v -> startCustomExam());
    }

    private void populateTaxonomyMultiSelectChips(List<TaxonomyItem> subjects, List<TaxonomyItem> tags) {
        if (subjects != null) {
            for (TaxonomyItem sub : subjects) {
                Chip chip = new Chip(requireContext());
                chip.setText(sub.getTitle());
                chip.setCheckable(true);
                chip.setOnCheckedChangeListener((btn, isChecked) -> {
                    if (isChecked) {
                        selectedSubjectIds.add(sub.getId());
                    } else {
                        selectedSubjectIds.remove(sub.getId());
                    }
                });
                cgTaxonomyMultiSelect.addView(chip);
            }
        }
        if (tags != null) {
            for (TaxonomyItem tag : tags) {
                Chip chip = new Chip(requireContext());
                chip.setText("#" + tag.getTitle());
                chip.setCheckable(true);
                chip.setOnCheckedChangeListener((btn, isChecked) -> {
                    if (isChecked) {
                        selectedTagNames.add(tag.getTitle());
                    } else {
                        selectedTagNames.remove(tag.getTitle());
                    }
                });
                cgTaxonomyMultiSelect.addView(chip);
            }
        }
    }

    private void startCustomExam() {
        String title = etCustomExamTitle.getText() != null ? etCustomExamTitle.getText().toString().trim() : "Custom Exam";
        String questionsCountStr = etTotalQuestions.getText() != null ? etTotalQuestions.getText().toString().trim() : "25";
        String durationStr = etExamDuration.getText() != null ? etExamDuration.getText().toString().trim() : "20";

        int totalQuestions = TextUtils.isEmpty(questionsCountStr) ? 25 : Integer.parseInt(questionsCountStr);
        int durationMinutes = TextUtils.isEmpty(durationStr) ? 20 : Integer.parseInt(durationStr);

        // Determine difficulty selection
        String difficulty = "all";
        int checkedChipId = cgDifficultySelector.getCheckedChipId();
        if (checkedChipId == R.id.chipDiffEasy) difficulty = "easy";
        else if (checkedChipId == R.id.chipDiffMedium) difficulty = "medium";
        else if (checkedChipId == R.id.chipDiffHard) difficulty = "hard";

        CustomExamRequest request = new CustomExamRequest();
        request.setTitle(title);
        request.setSubjectIds(selectedSubjectIds);
        request.setTagNames(selectedTagNames);
        request.setDifficulty(difficulty);
        request.setNegativeMarking(selectedNegativeMarking);
        request.setTotalQuestions(totalQuestions);
        request.setDurationMinutes(durationMinutes);

        btnStartCustomExam.setEnabled(false);
        pbExamGenerating.setVisibility(View.VISIBLE);

        examRepository.generateCustomExam(request, new ExamRepository.ExamCallback<CustomExamGenerateResponse>() {
            @Override
            public void onSuccess(CustomExamGenerateResponse response) {
                if (isAdded()) {
                    btnStartCustomExam.setEnabled(true);
                    pbExamGenerating.setVisibility(View.GONE);

                    Intent intent = new Intent(requireContext(), ExamArenaActivity.class);
                    intent.putExtra(ExamArenaActivity.EXTRA_EXAM_DATA, response);
                    startActivity(intent);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    btnStartCustomExam.setEnabled(true);
                    pbExamGenerating.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerAdapter != null && bannerAdapter.getItemCount() > 1) {
            carouselHandler.postDelayed(carouselRunnable, 4000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        carouselHandler.removeCallbacks(carouselRunnable);
    }
}
