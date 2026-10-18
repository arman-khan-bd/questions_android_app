package com.questions.govt.ui.main;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.questions.govt.BuildConfig;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.interceptor.AccountStatusInterceptor;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.SearchSuggestionsResponse;
import com.questions.govt.data.repository.NotificationRepository;
import com.questions.govt.data.repository.TaxonomyRepository;
import com.questions.govt.ui.auth.LoginActivity;
import com.questions.govt.ui.leaderboard.LeaderboardFragment;
import com.questions.govt.ui.notification.NotificationActivity;
import com.questions.govt.ui.profile.ProfileFragment;
import com.questions.govt.ui.question.CreateQuestionActivity;
import com.questions.govt.ui.question.QuestionsFragment;
import com.questions.govt.ui.search.SearchResultActivity;
import com.questions.govt.ui.search.SearchSuggestionAdapter;
import com.questions.govt.ui.subject.SubjectDetailActivity;
import com.questions.govt.ui.statistics.StatisticsFragment;
import com.questions.govt.util.AccountStatusDialogHelper;
import com.questions.govt.util.FeatureFlagManager;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity hosting Top Search Dropdown, 5-item Bottom Navigation, and Left Navigation Drawer.
 */
public class MainActivity extends AppCompatActivity {

    // Navigation Drawer
    private DrawerLayout drawerLayout;

    // Top App Bar
    private ImageView btnNavMenu;
    private ImageView ivHeaderLogo;
    private TextView tvHeaderTitle;
    private ImageView btnSearchToggle;
    private ImageView btnLeaderboardShortcut;
    private FrameLayout layoutNotificationContainer;
    private TextView tvNotificationBadge;

    // Sliding Top Search Panel
    private LinearLayout layoutSearchDropdown;
    private EditText etSearchInput;
    private ImageView ivCloseSearch;
    private TextView tvSuggestionsHeader;
    private RecyclerView rvSearchSuggestions;
    private ChipGroup cgSearchTags;
    private SearchSuggestionAdapter searchSuggestionAdapter;

    // Bottom Navigation
    private BottomNavigationView bottomNav;

    // Drawer Views (Web3 Card & Pill Architecture)
    private ImageView ivDrawerLogo;
    private TextView tvDrawerAppName;
    private ChipGroup cgDrawerPopularSearches;
    private ChipGroup cgDrawerTopics;
    private ChipGroup cgDrawerCategories;
    private ChipGroup cgDrawerTags;
    private RecyclerView rvDrawerSubjects;
    private TextView tvDrawerSubjectsCount;
    private LinearLayout layoutDrawerSaved;
    private SwitchCompat switchDarkMode;
    private LinearLayout layoutDrawerLogout;
    private TextView tvDrawerVersion;

    private DrawerSubjectCardAdapter drawerSubjectsCardAdapter;

    private TaxonomyRepository taxonomyRepository;
    private NotificationRepository notificationRepository;

    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    private BroadcastReceiver accountStatusReceiver;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    syncFirebaseToken();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taxonomyRepository = TaxonomyRepository.getInstance(this);
        notificationRepository = NotificationRepository.getInstance(this);

        initViews();
        setupTopSearchBar();
        setupBottomNav();
        setupLeftDrawer();
        setupNotificationBadgeObserver();
        registerAccountStatusReceiver();
        applyDynamicBranding();
        setupFirebaseCloudMessaging();

        // Load default HomeFragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        btnNavMenu = findViewById(R.id.btnNavMenu);
        if (btnNavMenu != null) {
            btnNavMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
        ivHeaderLogo = findViewById(R.id.ivHeaderLogo);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnSearchToggle = findViewById(R.id.btnSearchToggle);
        btnLeaderboardShortcut = findViewById(R.id.btnLeaderboardShortcut);
        layoutNotificationContainer = findViewById(R.id.layoutNotificationContainer);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        layoutSearchDropdown = findViewById(R.id.layoutSearchDropdown);
        etSearchInput = findViewById(R.id.etSearchInput);
        ivCloseSearch = findViewById(R.id.ivCloseSearch);
        tvSuggestionsHeader = findViewById(R.id.tvSuggestionsHeader);
        rvSearchSuggestions = findViewById(R.id.rvSearchSuggestions);
        cgSearchTags = findViewById(R.id.cgSearchTags);

        bottomNav = findViewById(R.id.bottomNav);

        ivDrawerLogo = findViewById(R.id.ivDrawerLogo);
        tvDrawerAppName = findViewById(R.id.tvDrawerAppName);
        cgDrawerPopularSearches = findViewById(R.id.cgDrawerPopularSearches);
        cgDrawerTopics = findViewById(R.id.cgDrawerTopics);
        cgDrawerCategories = findViewById(R.id.cgDrawerCategories);
        cgDrawerTags = findViewById(R.id.cgDrawerTags);
        rvDrawerSubjects = findViewById(R.id.rvDrawerSubjects);
        tvDrawerSubjectsCount = findViewById(R.id.tvDrawerSubjectsCount);
        layoutDrawerSaved = findViewById(R.id.layoutDrawerSaved);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        layoutDrawerLogout = findViewById(R.id.layoutDrawerLogout);
        tvDrawerVersion = findViewById(R.id.tvDrawerVersion);

        String versionText = getString(R.string.app_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        tvDrawerVersion.setText(versionText);
    }

    private void applyDynamicBranding() {
        FeatureFlagManager flagManager = FeatureFlagManager.getInstance();
        tvHeaderTitle.setText(flagManager.getAppName());
        tvDrawerAppName.setText(flagManager.getAppName());

        String logoUrl = flagManager.getHeaderLogoUrl();
        if (TextUtils.isEmpty(logoUrl)) {
            logoUrl = flagManager.getLogoUrl();
        }
        if (!TextUtils.isEmpty(logoUrl)) {
            Glide.with(this)
                    .load(logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(ivHeaderLogo);

            Glide.with(this)
                    .load(logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(ivDrawerLogo);
        }
    }

    private void setupTopSearchBar() {
        btnSearchToggle.setOnClickListener(v -> toggleSearchDropdown());
        ivCloseSearch.setOnClickListener(v -> hideSearchDropdown());

        searchSuggestionAdapter = new SearchSuggestionAdapter(query -> {
            hideSearchDropdown();
            launchSearchResult(query);
        });
        rvSearchSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSearchSuggestions.setAdapter(searchSuggestionAdapter);

        // IME Action Search on keyboard
        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etSearchInput.getText() != null ? etSearchInput.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(query)) {
                    hideSearchDropdown();
                    launchSearchResult(query);
                }
                return true;
            }
            return false;
        });

        // Debounced typing suggestions (300ms)
        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                if (searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }
                searchDebounceRunnable = () -> {
                    String query = s != null ? s.toString().trim() : "";
                    fetchSearchSuggestions(query);
                };
                searchDebounceHandler.postDelayed(searchDebounceRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Top Bar Shortcuts
        ivHeaderLogo.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnLeaderboardShortcut.setOnClickListener(v -> loadFragment(new LeaderboardFragment()));

        layoutNotificationContainer.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            startActivity(intent);
        });
    }

    private void toggleSearchDropdown() {
        if (layoutSearchDropdown.getVisibility() == View.VISIBLE) {
            hideSearchDropdown();
        } else {
            layoutSearchDropdown.setVisibility(View.VISIBLE);
            etSearchInput.requestFocus();
            fetchSearchSuggestions("");
        }
    }

    private void hideSearchDropdown() {
        layoutSearchDropdown.setVisibility(View.GONE);
        etSearchInput.setText("");
    }

    private void fetchSearchSuggestions(String query) {
        ApiClient.getApiService().getSearchSuggestions(query).enqueue(new Callback<ApiResponse<SearchSuggestionsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SearchSuggestionsResponse>> call, Response<ApiResponse<SearchSuggestionsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    SearchSuggestionsResponse data = response.body().getData();
                    tvSuggestionsHeader.setText(TextUtils.isEmpty(query) ? "Popular Searches" : "Suggestions");
                    searchSuggestionAdapter.updateData(data.getPopularSearches());

                    cgSearchTags.removeAllViews();
                    if (data.getTags() != null) {
                        for (String tag : data.getTags()) {
                            Chip chip = new Chip(MainActivity.this);
                            String displayTag = tag.startsWith("#") ? tag : "#" + tag;
                            String cleanTag = tag.replaceFirst("^#", "").trim();
                            chip.setText(displayTag);
                            chip.setChipBackgroundColorResource(R.color.surface_variant);
                            chip.setOnClickListener(v -> {
                                hideSearchDropdown();
                                launchSearchTag(cleanTag);
                            });
                            cgSearchTags.addView(chip);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SearchSuggestionsResponse>> call, Throwable t) {}
        });
    }

    private void launchSearchResult(String query) {
        Intent intent = new Intent(this, SearchResultActivity.class);
        if (query != null && query.trim().startsWith("#")) {
            String cleanTag = query.trim().substring(1).trim();
            intent.putExtra(SearchResultActivity.EXTRA_TAG, cleanTag);
            intent.putExtra(SearchResultActivity.EXTRA_TITLE, "#" + cleanTag);
        } else {
            intent.putExtra(SearchResultActivity.EXTRA_QUERY, query);
            intent.putExtra(SearchResultActivity.EXTRA_TITLE, query);
        }
        startActivity(intent);
    }

    private void launchSearchTag(String tag) {
        String cleanTag = tag != null ? tag.replaceFirst("^#", "").trim() : "";
        Intent intent = new Intent(this, SearchResultActivity.class);
        intent.putExtra(SearchResultActivity.EXTRA_TAG, cleanTag);
        intent.putExtra(SearchResultActivity.EXTRA_TITLE, "#" + cleanTag);
        startActivity(intent);
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_questions) {
                loadFragment(new QuestionsFragment());
                return true;
            } else if (itemId == R.id.nav_statistics) {
                loadFragment(new StatisticsFragment());
                return true;
            } else if (itemId == R.id.nav_exam_plus) {
                showExamPlusBottomSheet();
                return false; // Do not select as a standard tab
            } else if (itemId == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void showExamPlusBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_exam_plus, null);
        dialog.setContentView(sheetView);

        View cardCustomExam = sheetView.findViewById(R.id.cardOptionCustomExam);
        View cardCreateQuestion = sheetView.findViewById(R.id.cardOptionCreateQuestion);
        View cardOptionAllQuestions = sheetView.findViewById(R.id.cardOptionAllQuestions);

        // Feature flag: hide community question creation if disabled by Admin
        boolean canCreateQuestion = FeatureFlagManager.getInstance().isCommunityQuestionCreationEnabled();
        cardCreateQuestion.setVisibility(canCreateQuestion ? View.VISIBLE : View.GONE);

        cardCustomExam.setOnClickListener(v -> {
            dialog.dismiss();
            bottomNav.setSelectedItemId(R.id.nav_questions);
            loadFragment(QuestionsFragment.newInstance(true));
        });

        cardCreateQuestion.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, CreateQuestionActivity.class);
            startActivity(intent);
        });

        if (cardOptionAllQuestions != null) {
            cardOptionAllQuestions.setOnClickListener(v -> {
                dialog.dismiss();
                bottomNav.setSelectedItemId(R.id.nav_questions);
            });
        }

        dialog.show();
    }

    private void setupLeftDrawer() {
        // 1. Subjects (Modern Web3 2-column Card View)
        drawerSubjectsCardAdapter = new DrawerSubjectCardAdapter(sub -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(MainActivity.this, SubjectDetailActivity.class);
            intent.putExtra(SubjectDetailActivity.EXTRA_SUBJECT_ID, sub.getId());
            intent.putExtra(SubjectDetailActivity.EXTRA_SUBJECT_NAME, sub.getTitle());
            intent.putExtra(SubjectDetailActivity.EXTRA_SUBJECT_COLOR, sub.getColor());
            startActivity(intent);
        });
        rvDrawerSubjects.setLayoutManager(new GridLayoutManager(this, 2));
        rvDrawerSubjects.setAdapter(drawerSubjectsCardAdapter);

        // Populate drawer subjects from Room repository
        taxonomyRepository.getSubjects().observe(this, list -> {
            if (list != null) {
                drawerSubjectsCardAdapter.updateData(list);
                if (tvDrawerSubjectsCount != null) {
                    tvDrawerSubjectsCount.setText(list.size() + " Total");
                }
            }
        });

        // 2. Topics (Pill View)
        taxonomyRepository.getTopics().observe(this, this::populateDrawerTopicPills);

        // 3. Categories (Pill View)
        taxonomyRepository.getCategories().observe(this, this::populateDrawerCategoryPills);

        // 4. Tags (Pill View)
        taxonomyRepository.getTags().observe(this, this::populateDrawerTagPills);

        // 5. Popular Searches from Home Feed or Suggestions
        ApiClient.getApiService().getSearchSuggestions("").enqueue(new Callback<ApiResponse<SearchSuggestionsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SearchSuggestionsResponse>> call, Response<ApiResponse<SearchSuggestionsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    populateDrawerSearchPills(response.body().getData().getPopularSearches());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<SearchSuggestionsResponse>> call, Throwable t) {}
        });

        // 6. Footer Actions
        layoutDrawerSaved.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(MainActivity.this, SearchResultActivity.class);
            intent.putExtra(SearchResultActivity.EXTRA_IS_STARRED, true);
            intent.putExtra(SearchResultActivity.EXTRA_TITLE, "Saved Questions");
            startActivity(intent);
        });

        switchDarkMode.setChecked(SessionManager.getInstance().isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            SessionManager.getInstance().setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        layoutDrawerLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void populateDrawerTopicPills(List<TaxonomyItem> topics) {
        if (cgDrawerTopics == null) return;
        cgDrawerTopics.removeAllViews();
        if (topics == null || topics.isEmpty()) return;
        for (TaxonomyItem topic : topics) {
            Chip chip = createWeb3PillChip(topic.getTitle(), () -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(MainActivity.this, SearchResultActivity.class);
                intent.putExtra(SearchResultActivity.EXTRA_TOPIC_ID, topic.getId());
                intent.putExtra(SearchResultActivity.EXTRA_TITLE, topic.getTitle());
                startActivity(intent);
            });
            cgDrawerTopics.addView(chip);
        }
    }

    private void populateDrawerCategoryPills(List<TaxonomyItem> categories) {
        if (cgDrawerCategories == null) return;
        cgDrawerCategories.removeAllViews();
        if (categories == null || categories.isEmpty()) return;
        for (TaxonomyItem cat : categories) {
            Chip chip = createWeb3PillChip(cat.getTitle(), () -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(MainActivity.this, SearchResultActivity.class);
                intent.putExtra(SearchResultActivity.EXTRA_CATEGORY_ID, cat.getId());
                intent.putExtra(SearchResultActivity.EXTRA_TITLE, cat.getTitle());
                startActivity(intent);
            });
            cgDrawerCategories.addView(chip);
        }
    }

    private void populateDrawerTagPills(List<TaxonomyItem> tags) {
        if (cgDrawerTags == null) return;
        cgDrawerTags.removeAllViews();
        if (tags == null || tags.isEmpty()) return;
        for (TaxonomyItem tag : tags) {
            String cleanTag = tag.getTitle() != null ? tag.getTitle().replaceFirst("^#", "").trim() : "";
            Chip chip = createWeb3PillChip("#" + cleanTag, () -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                launchSearchTag(cleanTag);
            });
            cgDrawerTags.addView(chip);
        }
    }

    private void populateDrawerSearchPills(List<String> searches) {
        if (cgDrawerPopularSearches == null) return;
        cgDrawerPopularSearches.removeAllViews();
        if (searches == null || searches.isEmpty()) return;
        for (String query : searches) {
            Chip chip = createWeb3PillChip(query, () -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                launchSearchResult(query);
            });
            cgDrawerPopularSearches.addView(chip);
        }
    }

    private Chip createWeb3PillChip(String text, Runnable onClick) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setChipCornerRadius(28f);
        chip.setChipBackgroundColorResource(R.color.surface_variant);
        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        chip.setChipStrokeColorResource(R.color.border);
        chip.setChipStrokeWidth(1.5f);
        chip.setTextSize(12f);
        chip.setOnClickListener(v -> onClick.run());
        return chip;
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to sign out from Govt Exam Prep?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    ApiClient.getApiService().logout().enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
                    });
                    SessionManager.getInstance().clearSession();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupNotificationBadgeObserver() {
        notificationRepository.getUnreadCount().observe(this, count -> {
            if (count != null && count > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    private void registerAccountStatusReceiver() {
        accountStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra(AccountStatusInterceptor.EXTRA_STATUS);
                String reason = intent.getStringExtra(AccountStatusInterceptor.EXTRA_REASON);
                String expires = intent.getStringExtra(AccountStatusInterceptor.EXTRA_EXPIRES);

                if ("suspended".equalsIgnoreCase(status)) {
                    AccountStatusDialogHelper.showAccountSuspended(MainActivity.this, reason, expires);
                } else if ("disabled".equalsIgnoreCase(status)) {
                    AccountStatusDialogHelper.showAccountDisabled(MainActivity.this, reason);
                }
            }
        };

        IntentFilter filter = new IntentFilter(AccountStatusInterceptor.ACTION_ACCOUNT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(accountStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(accountStatusReceiver, filter);
        }
    }

    public void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (layoutSearchDropdown.getVisibility() == View.VISIBLE) {
            hideSearchDropdown();
        } else {
            super.onBackPressed();
        }
    }

    private void setupFirebaseCloudMessaging() {
        // Subscribe to global topics for exam announcements and app updates
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all");
            FirebaseMessaging.getInstance().subscribeToTopic("exams");
        } catch (Exception e) {
            android.util.Log.w("MainActivity", "Error subscribing to FCM topics", e);
        }

        // Check/request POST_NOTIFICATIONS permission on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                syncFirebaseToken();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            syncFirebaseToken();
        }
    }

    private void syncFirebaseToken() {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) {
                    android.util.Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                SessionManager.getInstance().saveFcmToken(token);

                if (SessionManager.getInstance().isLoggedIn()) {
                    Map<String, String> body = new HashMap<>();
                    body.put("fcm_token", token);
                    ApiClient.getApiService().syncFcmToken(body).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                            android.util.Log.d("MainActivity", "FCM token synced with backend");
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            android.util.Log.w("MainActivity", "Failed to sync FCM token: " + t.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            android.util.Log.w("MainActivity", "Error retrieving FCM token", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (accountStatusReceiver != null) {
            try {
                unregisterReceiver(accountStatusReceiver);
            } catch (Exception ignored) {}
        }
    }
}
