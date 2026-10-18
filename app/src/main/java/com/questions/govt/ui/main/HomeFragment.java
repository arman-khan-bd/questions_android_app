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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.questions.govt.R;
import com.questions.govt.data.model.BannerItem;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.remote.response.HomeFeedResponse;
import com.questions.govt.data.repository.TaxonomyRepository;
import com.questions.govt.ui.exam.ExamArenaActivity;
import com.questions.govt.ui.search.SearchResultActivity;

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

    private BannerCarouselAdapter bannerAdapter;
    private TopicAdapter topicAdapter;
    private SubjectGridAdapter subjectAdapter;

    private TaxonomyRepository taxonomyRepository;

    private final Handler carouselHandler = new Handler(Looper.getMainLooper());
    private Runnable carouselRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taxonomyRepository = TaxonomyRepository.getInstance(requireContext());

        initViews(view);
        setupAdapters();
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
                    if (response != null) {
                        if (response.getBanners() != null) {
                            bannerAdapter.setBanners(response.getBanners());
                            carouselHandler.removeCallbacks(carouselRunnable);
                            carouselHandler.postDelayed(carouselRunnable, 4000);
                        }
                        if (response.getSubjects() != null && !response.getSubjects().isEmpty()) {
                            subjectAdapter.setSubjects(response.getSubjects());
                            tvSubjectsCountBadge.setText(response.getSubjects().size() + " Subjects");
                        }
                        if (response.getTopics() != null && !response.getTopics().isEmpty()) {
                            topicAdapter.setTopics(response.getTopics());
                        }
                        if (response.getCategories() != null && !response.getCategories().isEmpty()) {
                            populateCategoryChips(response.getCategories());
                        }
                        if (response.getTags() != null && !response.getTags().isEmpty()) {
                            populateTagChips(response.getTags());
                        }
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
