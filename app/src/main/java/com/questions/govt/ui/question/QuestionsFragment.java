package com.questions.govt.ui.question;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.questions.govt.R;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.PaginatedList;
import com.questions.govt.data.repository.QuestionRepository;
import com.questions.govt.data.repository.TaxonomyRepository;
import com.questions.govt.util.FeatureFlagManager;
import com.questions.govt.util.NetworkErrorParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * All Questions Fragment hosting complete question bank browsing,
 * dynamic subject & difficulty filtering, search, sorting, and infinite scroll.
 */
public class QuestionsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvQuestions;
    private TextView tvQuestionsCountSubtitle;
    private ImageView btnRefreshQuestions;
    private EditText etQuestionSearch;
    private ImageView btnClearSearch;
    private ChipGroup cgDifficultyFilters;
    private ChipGroup cgSubjectFilters;
    private ChipGroup cgSortOptions;
    private LinearLayout layoutEmptyState;
    private Button btnResetFilters;
    private ProgressBar pbInitialLoading;
    private ProgressBar pbPagination;
    private FloatingActionButton fabCreateQuestion;

    private QuestionCardAdapter adapter;
    private QuestionRepository questionRepository;
    private TaxonomyRepository taxonomyRepository;

    private String searchQuery = "";
    private String selectedDifficulty = null;
    private Long selectedSubjectId = null;
    private String selectedSort = "recent";

    private int currentPage = 1;
    private int lastPage = 1;
    private int totalQuestions = 0;
    private boolean isLoading = false;

    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        questionRepository = QuestionRepository.getInstance(requireContext());
        taxonomyRepository = TaxonomyRepository.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();
        setupFilters();
        setupSearch();
        loadSubjects();
        loadQuestions(1, true);
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rvQuestions = view.findViewById(R.id.rvQuestions);
        tvQuestionsCountSubtitle = view.findViewById(R.id.tvQuestionsCountSubtitle);
        btnRefreshQuestions = view.findViewById(R.id.btnRefreshQuestions);
        etQuestionSearch = view.findViewById(R.id.etQuestionSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        cgDifficultyFilters = view.findViewById(R.id.cgDifficultyFilters);
        cgSubjectFilters = view.findViewById(R.id.cgSubjectFilters);
        cgSortOptions = view.findViewById(R.id.cgSortOptions);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        btnResetFilters = view.findViewById(R.id.btnResetFilters);
        pbInitialLoading = view.findViewById(R.id.pbInitialLoading);
        pbPagination = view.findViewById(R.id.pbPagination);
        fabCreateQuestion = view.findViewById(R.id.fabCreateQuestion);

        // Feature flag for creating question
        boolean canCreateQuestion = FeatureFlagManager.getInstance().isCommunityQuestionCreationEnabled();
        fabCreateQuestion.setVisibility(canCreateQuestion ? View.VISIBLE : View.GONE);
        fabCreateQuestion.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateQuestionActivity.class);
            startActivity(intent);
        });

        btnRefreshQuestions.setOnClickListener(v -> {
            swipeRefresh.setRefreshing(true);
            loadQuestions(1, true);
        });

        btnResetFilters.setOnClickListener(v -> resetAllFilters());

        swipeRefresh.setOnRefreshListener(() -> loadQuestions(1, true));
    }

    private void setupRecyclerView() {
        adapter = new QuestionCardAdapter(requireContext(), getChildFragmentManager());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvQuestions.setLayoutManager(layoutManager);
        rvQuestions.setAdapter(adapter);

        // Infinite Scroll Pagination
        rvQuestions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoading && currentPage < lastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        loadQuestions(currentPage + 1, false);
                    }
                }
            }
        });
    }

    private void setupFilters() {
        // Difficulty Filter Selection
        cgDifficultyFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipDiffEasy) {
                selectedDifficulty = "Easy";
            } else if (checkedId == R.id.chipDiffMedium) {
                selectedDifficulty = "Medium";
            } else if (checkedId == R.id.chipDiffHard) {
                selectedDifficulty = "Hard";
            } else {
                selectedDifficulty = null; // All
            }
            loadQuestions(1, true);
        });

        // Sort Options Selection
        cgSortOptions.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipSortPopular) {
                selectedSort = "popular";
            } else if (checkedId == R.id.chipSortAttempts) {
                selectedSort = "attempts";
            } else {
                selectedSort = "recent";
            }
            loadQuestions(1, true);
        });
    }

    private void setupSearch() {
        btnClearSearch.setOnClickListener(v -> {
            etQuestionSearch.setText("");
            searchQuery = "";
            btnClearSearch.setVisibility(View.GONE);
            loadQuestions(1, true);
        });

        etQuestionSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchQuery = etQuestionSearch.getText() != null ? etQuestionSearch.getText().toString().trim() : "";
                loadQuestions(1, true);
                return true;
            }
            return false;
        });

        etQuestionSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                String text = s != null ? s.toString().trim() : "";
                btnClearSearch.setVisibility(!text.isEmpty() ? View.VISIBLE : View.GONE);

                if (searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }
                searchDebounceRunnable = () -> {
                    searchQuery = text;
                    loadQuestions(1, true);
                };
                searchDebounceHandler.postDelayed(searchDebounceRunnable, 350);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSubjects() {
        taxonomyRepository.getSubjects().observe(getViewLifecycleOwner(), subjects -> {
            if (subjects == null || subjects.isEmpty()) return;

            // Keep "All Subjects" chip
            View allChip = cgSubjectFilters.findViewById(R.id.chipSubjectAll);
            cgSubjectFilters.removeAllViews();
            if (allChip != null) {
                cgSubjectFilters.addView(allChip);
            }

            for (TaxonomyItem subject : subjects) {
                Chip chip = new Chip(requireContext());
                chip.setText(subject.getTitle());
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setTextSize(12f);
                chip.setTag(subject.getId());

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedSubjectId = (Long) buttonView.getTag();
                        loadQuestions(1, true);
                    }
                });

                cgSubjectFilters.addView(chip);
            }

            // Restore All Subjects handler
            Chip chipAll = cgSubjectFilters.findViewById(R.id.chipSubjectAll);
            if (chipAll != null) {
                chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedSubjectId = null;
                        loadQuestions(1, true);
                    }
                });
            }
        });
    }

    public void loadQuestions(int page, boolean reset) {
        if (isLoading) return;
        isLoading = true;

        if (reset) {
            currentPage = 1;
            if (!swipeRefresh.isRefreshing()) {
                pbInitialLoading.setVisibility(View.VISIBLE);
            }
        } else {
            pbPagination.setVisibility(View.VISIBLE);
        }

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("sort", selectedSort);

        if (!TextUtils.isEmpty(searchQuery)) {
            params.put("q", searchQuery);
        }
        if (!TextUtils.isEmpty(selectedDifficulty)) {
            params.put("difficulty", selectedDifficulty);
        }
        if (selectedSubjectId != null && selectedSubjectId > 0) {
            params.put("subject_id", String.valueOf(selectedSubjectId));
        }

        ApiClient.getApiService().searchQuestions(params).enqueue(new Callback<ApiResponse<PaginatedList<Question>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedList<Question>>> call, Response<ApiResponse<PaginatedList<Question>>> response) {
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                pbInitialLoading.setVisibility(View.GONE);
                pbPagination.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    PaginatedList<Question> paginatedList = response.body().getData();
                    List<Question> questions = paginatedList.getData();

                    currentPage = paginatedList.getCurrentPage();
                    lastPage = paginatedList.getLastPage();
                    totalQuestions = paginatedList.getTotal();

                    // Update UI counter subtitle
                    updateSubtitleCounter();

                    if (reset) {
                        adapter.setQuestions(questions);
                        if (questions == null || questions.isEmpty()) {
                            layoutEmptyState.setVisibility(View.VISIBLE);
                            rvQuestions.setVisibility(View.GONE);
                        } else {
                            layoutEmptyState.setVisibility(View.GONE);
                            rvQuestions.setVisibility(View.VISIBLE);
                            rvQuestions.scrollToPosition(0);
                        }
                    } else {
                        adapter.addQuestions(questions);
                    }

                    // Cache to local Room database
                    if (questions != null && !questions.isEmpty()) {
                        questionRepository.cacheQuestions(questions);
                    }
                } else {
                    handleFetchError(response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaginatedList<Question>>> call, Throwable t) {
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                pbInitialLoading.setVisibility(View.GONE);
                pbPagination.setVisibility(View.GONE);

                String errorMsg = NetworkErrorParser.parseFailure(t, getString(R.string.network_error));
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();

                if (reset && (adapter.getItemCount() == 0)) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void handleFetchError(Response<?> response) {
        String msg = NetworkErrorParser.parseError(response, "Unable to load questions.");
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        if (adapter.getItemCount() == 0) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void updateSubtitleCounter() {
        if (tvQuestionsCountSubtitle == null) return;
        if (totalQuestions > 0) {
            tvQuestionsCountSubtitle.setText(getString(R.string.all_questions_subtitle) + " (" + totalQuestions + ")");
        } else {
            tvQuestionsCountSubtitle.setText(getString(R.string.all_questions_subtitle));
        }
    }

    private void resetAllFilters() {
        searchQuery = "";
        selectedDifficulty = null;
        selectedSubjectId = null;
        selectedSort = "recent";

        etQuestionSearch.setText("");
        btnClearSearch.setVisibility(View.GONE);

        Chip chipDiffAll = cgDifficultyFilters.findViewById(R.id.chipDiffAll);
        if (chipDiffAll != null) chipDiffAll.setChecked(true);

        Chip chipSubjectAll = cgSubjectFilters.findViewById(R.id.chipSubjectAll);
        if (chipSubjectAll != null) chipSubjectAll.setChecked(true);

        Chip chipSortLatest = cgSortOptions.findViewById(R.id.chipSortLatest);
        if (chipSortLatest != null) chipSortLatest.setChecked(true);

        loadQuestions(1, true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchDebounceRunnable != null) {
            searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
        }
    }
}
