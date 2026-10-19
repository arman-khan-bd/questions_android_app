package com.questions.govt.ui.question;

import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.questions.govt.R;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.TaxonomyItem;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.CustomExamGenerateResponse;
import com.questions.govt.data.remote.response.PaginatedList;
import com.questions.govt.data.repository.QuestionRepository;
import com.questions.govt.data.repository.TaxonomyRepository;
import com.questions.govt.ui.exam.ExamArenaActivity;
import com.questions.govt.util.ExamSessionManager;
import com.questions.govt.util.NetworkErrorParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Modern Question Bank Fragment:
 * - Collapsible search bar toggled via header icon button.
 * - Compact "Filter & Sort" button with active badge + active chips.
 * - Question selection from bank for custom self-test exams.
 * - Small docked floating exam button above bottom navigation bar.
 * - Rich Self-Test Builder Bottom Sheet with negative cut marks and timer options.
 */
public class QuestionsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvQuestions;
    private ImageView btnToggleSearch;
    private ImageView btnRefreshQuestions;

    // Search bar container
    private CardView layoutSearchContainer;
    private EditText etQuestionSearch;
    private ImageView btnClearSearch;
    private ImageView btnCloseSearch;

    // Compact Filter & Sort Bar
    private View btnFilterAndSort;
    private TextView tvActiveFilterBadge;
    private ChipGroup cgActiveFilterChips;
    private Chip chipActiveDifficulty;
    private Chip chipActiveSubject;
    private Chip chipActiveSort;
    private TextView btnSelectAllToggle;

    // Status & FAB
    private LinearLayout layoutEmptyState;
    private Button btnResetFilters;
    private ProgressBar pbInitialLoading;
    private ProgressBar pbPagination;
    private ExtendedFloatingActionButton fabCreateExamFromQuestions;

    private QuestionCardAdapter adapter;
    private QuestionRepository questionRepository;
    private TaxonomyRepository taxonomyRepository;

    private final List<TaxonomyItem> loadedSubjects = new ArrayList<>();

    private String searchQuery = "";
    private String selectedDifficulty = null;
    private Long selectedSubjectId = null;
    private String selectedSubjectName = null;
    private String selectedSort = "recent";

    private int currentPage = 1;
    private int lastPage = 1;
    private int totalQuestions = 0;
    private boolean isLoading = false;

    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

    public static final String ARG_OPEN_BUILDER = "arg_open_builder";

    public static QuestionsFragment newInstance(boolean openSelfTestBuilder) {
        QuestionsFragment fragment = new QuestionsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_OPEN_BUILDER, openSelfTestBuilder);
        fragment.setArguments(args);
        return fragment;
    }

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
        setupSearch();
        setupFilterBar();
        loadSubjects();
        loadQuestions(1, true);

        if (getArguments() != null && getArguments().getBoolean(ARG_OPEN_BUILDER, false)) {
            view.post(this::showSelfTestBuilderBottomSheet);
        }
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rvQuestions = view.findViewById(R.id.rvQuestions);
        btnToggleSearch = view.findViewById(R.id.btnToggleSearch);
        btnRefreshQuestions = view.findViewById(R.id.btnRefreshQuestions);

        layoutSearchContainer = view.findViewById(R.id.layoutSearchContainer);
        etQuestionSearch = view.findViewById(R.id.etQuestionSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        btnCloseSearch = view.findViewById(R.id.btnCloseSearch);

        btnFilterAndSort = view.findViewById(R.id.btnFilterAndSort);
        tvActiveFilterBadge = view.findViewById(R.id.tvActiveFilterBadge);
        cgActiveFilterChips = view.findViewById(R.id.cgActiveFilterChips);
        chipActiveDifficulty = view.findViewById(R.id.chipActiveDifficulty);
        chipActiveSubject = view.findViewById(R.id.chipActiveSubject);
        chipActiveSort = view.findViewById(R.id.chipActiveSort);
        btnSelectAllToggle = view.findViewById(R.id.btnSelectAllToggle);

        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        btnResetFilters = view.findViewById(R.id.btnResetFilters);
        pbInitialLoading = view.findViewById(R.id.pbInitialLoading);
        pbPagination = view.findViewById(R.id.pbPagination);
        fabCreateExamFromQuestions = view.findViewById(R.id.fabCreateExamFromQuestions);

        // Header controls
        btnRefreshQuestions.setOnClickListener(v -> {
            swipeRefresh.setRefreshing(true);
            loadQuestions(1, true);
        });

        btnResetFilters.setOnClickListener(v -> resetAllFilters());
        swipeRefresh.setOnRefreshListener(() -> loadQuestions(1, true));

        // Floating Exam Pill Click -> Opens Self-Test Builder Bottom Sheet
        if (fabCreateExamFromQuestions != null) {
            fabCreateExamFromQuestions.hide();
            fabCreateExamFromQuestions.setOnClickListener(v -> showSelfTestBuilderBottomSheet());
        }
    }

    private void setupRecyclerView() {
        adapter = new QuestionCardAdapter(requireContext(), getChildFragmentManager());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvQuestions.setLayoutManager(layoutManager);
        rvQuestions.setAdapter(adapter);

        // Selection Change Listener
        adapter.setOnSelectionChangedListener((count, selectedList) -> updateSelectionUI(count));

        // Select All / Deselect All Toggle Button
        btnSelectAllToggle.setOnClickListener(v -> {
            if (adapter.getSelectedCount() > 0) {
                adapter.clearSelection();
            } else {
                adapter.selectAll();
            }
        });

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

    private void updateSelectionUI(int selectedCount) {
        if (btnSelectAllToggle != null) {
            btnSelectAllToggle.setText(selectedCount > 0 ? "Clear (" + selectedCount + ")" : "Select All");
        }
        if (fabCreateExamFromQuestions != null) {
            if (selectedCount > 0) {
                fabCreateExamFromQuestions.setText("Create Self-Exam (" + selectedCount + ")");
                fabCreateExamFromQuestions.show();
            } else {
                fabCreateExamFromQuestions.hide();
            }
        }
    }

    private void setupSearch() {
        // Search icon toggle button
        btnToggleSearch.setOnClickListener(v -> {
            if (layoutSearchContainer.getVisibility() == View.VISIBLE) {
                hideSearchInput();
            } else {
                showSearchInput();
            }
        });

        btnCloseSearch.setOnClickListener(v -> hideSearchInput());

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
                hideKeyboard();
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

    private void showSearchInput() {
        layoutSearchContainer.setVisibility(View.VISIBLE);
        etQuestionSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etQuestionSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideSearchInput() {
        layoutSearchContainer.setVisibility(View.GONE);
        hideKeyboard();
        if (!TextUtils.isEmpty(searchQuery)) {
            etQuestionSearch.setText("");
            searchQuery = "";
            loadQuestions(1, true);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etQuestionSearch != null) {
            imm.hideSoftInputFromWindow(etQuestionSearch.getWindowToken(), 0);
        }
    }

    private void setupFilterBar() {
        btnFilterAndSort.setOnClickListener(v -> showFilterAndSortBottomSheet());

        // Active Chip Closures
        chipActiveDifficulty.setOnCloseIconClickListener(v -> {
            selectedDifficulty = null;
            updateActiveFilterBadges();
            loadQuestions(1, true);
        });

        chipActiveSubject.setOnCloseIconClickListener(v -> {
            selectedSubjectId = null;
            selectedSubjectName = null;
            updateActiveFilterBadges();
            loadQuestions(1, true);
        });

        chipActiveSort.setOnCloseIconClickListener(v -> {
            selectedSort = "recent";
            updateActiveFilterBadges();
            loadQuestions(1, true);
        });

        updateActiveFilterBadges();
    }

    private void updateActiveFilterBadges() {
        int activeCount = 0;

        if (!TextUtils.isEmpty(selectedDifficulty)) {
            activeCount++;
            chipActiveDifficulty.setText(selectedDifficulty);
            chipActiveDifficulty.setVisibility(View.VISIBLE);
        } else {
            chipActiveDifficulty.setVisibility(View.GONE);
        }

        if (selectedSubjectId != null && !TextUtils.isEmpty(selectedSubjectName)) {
            activeCount++;
            chipActiveSubject.setText(selectedSubjectName);
            chipActiveSubject.setVisibility(View.VISIBLE);
        } else {
            chipActiveSubject.setVisibility(View.GONE);
        }

        if (!"recent".equals(selectedSort) && !TextUtils.isEmpty(selectedSort)) {
            activeCount++;
            String sortLabel = "Popular";
            if ("attempts".equals(selectedSort)) sortLabel = "Attempts";
            else if ("diff_asc".equals(selectedSort)) sortLabel = "Easy First";
            else if ("diff_desc".equals(selectedSort)) sortLabel = "Hard First";
            chipActiveSort.setText(sortLabel);
            chipActiveSort.setVisibility(View.VISIBLE);
        } else {
            chipActiveSort.setVisibility(View.GONE);
        }

        if (activeCount > 0) {
            tvActiveFilterBadge.setVisibility(View.VISIBLE);
            tvActiveFilterBadge.setText(String.valueOf(activeCount));
        } else {
            tvActiveFilterBadge.setVisibility(View.GONE);
        }
    }

    private void loadSubjects() {
        taxonomyRepository.getSubjects().observe(getViewLifecycleOwner(), subjects -> {
            if (subjects != null) {
                loadedSubjects.clear();
                loadedSubjects.addAll(subjects);
            }
        });
    }

    /**
     * Compact Filter & Sort Bottom Sheet
     */
    private void showFilterAndSortBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_questions_filter, null);
        dialog.setContentView(sheet);

        ChipGroup cgDifficulty = sheet.findViewById(R.id.cgSheetDifficulty);
        ChipGroup cgSubjects = sheet.findViewById(R.id.cgSheetSubjects);
        ChipGroup cgSort = sheet.findViewById(R.id.cgSheetSort);
        TextView btnReset = sheet.findViewById(R.id.btnSheetReset);
        Button btnApply = sheet.findViewById(R.id.btnSheetApply);

        // Pre-check Difficulty
        if ("Easy".equalsIgnoreCase(selectedDifficulty)) {
            cgDifficulty.check(R.id.chipSheetDiffEasy);
        } else if ("Medium".equalsIgnoreCase(selectedDifficulty)) {
            cgDifficulty.check(R.id.chipSheetDiffMedium);
        } else if ("Hard".equalsIgnoreCase(selectedDifficulty)) {
            cgDifficulty.check(R.id.chipSheetDiffHard);
        } else {
            cgDifficulty.check(R.id.chipSheetDiffAll);
        }

        // Dynamically add Subject chips
        cgSubjects.removeAllViews();
        Chip chipAllSubjects = new Chip(requireContext());
        chipAllSubjects.setId(View.generateViewId());
        chipAllSubjects.setText("All Subjects");
        chipAllSubjects.setCheckable(true);
        chipAllSubjects.setClickable(true);
        chipAllSubjects.setTextSize(12f);
        chipAllSubjects.setTag(null);
        cgSubjects.addView(chipAllSubjects);
        if (selectedSubjectId == null) {
            chipAllSubjects.setChecked(true);
        }

        for (TaxonomyItem subject : loadedSubjects) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(subject.getTitle());
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setTextSize(12f);
            chip.setTag(subject);
            cgSubjects.addView(chip);

            if (selectedSubjectId != null && selectedSubjectId.equals(subject.getId())) {
                chip.setChecked(true);
            }
        }

        // Pre-check Sort
        if ("popular".equals(selectedSort)) {
            cgSort.check(R.id.chipSheetSortPopular);
        } else if ("attempts".equals(selectedSort)) {
            cgSort.check(R.id.chipSheetSortAttempts);
        } else if ("diff_asc".equals(selectedSort)) {
            cgSort.check(R.id.chipSheetSortEasyFirst);
        } else if ("diff_desc".equals(selectedSort)) {
            cgSort.check(R.id.chipSheetSortHardFirst);
        } else {
            cgSort.check(R.id.chipSheetSortLatest);
        }

        // Reset
        btnReset.setOnClickListener(v -> {
            cgDifficulty.check(R.id.chipSheetDiffAll);
            chipAllSubjects.setChecked(true);
            cgSort.check(R.id.chipSheetSortLatest);
        });

        // Apply
        btnApply.setOnClickListener(v -> {
            // 1. Difficulty
            int diffId = cgDifficulty.getCheckedChipId();
            if (diffId == R.id.chipSheetDiffEasy) selectedDifficulty = "Easy";
            else if (diffId == R.id.chipSheetDiffMedium) selectedDifficulty = "Medium";
            else if (diffId == R.id.chipSheetDiffHard) selectedDifficulty = "Hard";
            else selectedDifficulty = null;

            // 2. Subject
            int subjectChipId = cgSubjects.getCheckedChipId();
            Chip checkedSubjectChip = sheet.findViewById(subjectChipId);
            if (checkedSubjectChip != null && checkedSubjectChip.getTag() instanceof TaxonomyItem) {
                TaxonomyItem item = (TaxonomyItem) checkedSubjectChip.getTag();
                selectedSubjectId = item.getId();
                selectedSubjectName = item.getTitle();
            } else {
                selectedSubjectId = null;
                selectedSubjectName = null;
            }

            // 3. Sort
            int sortId = cgSort.getCheckedChipId();
            if (sortId == R.id.chipSheetSortPopular) selectedSort = "popular";
            else if (sortId == R.id.chipSheetSortAttempts) selectedSort = "attempts";
            else if (sortId == R.id.chipSheetSortEasyFirst) selectedSort = "diff_asc";
            else if (sortId == R.id.chipSheetSortHardFirst) selectedSort = "diff_desc";
            else selectedSort = "recent";

            updateActiveFilterBadges();
            loadQuestions(1, true);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Self-Test Exam Builder Bottom Sheet:
     * - Allows choosing selected questions or filtered bank
     * - Configures negative cut mark (0.00, 0.20, 0.25, 0.50, 1.00)
     * - Sets custom exam title, duration, question shuffling
     * - In-memory exam session execution avoiding Parcelable/Binder crashes
     */
    private void showSelfTestBuilderBottomSheet() {
        List<Question> allQuestions = adapter != null ? adapter.getQuestions() : null;
        if (allQuestions == null || allQuestions.isEmpty()) {
            Toast.makeText(requireContext(), "No questions available in the current bank.", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_self_test_builder, null);
        dialog.setContentView(sheet);

        TextInputEditText etExamTitle = sheet.findViewById(R.id.etExamTitle);
        TextView tvExamSourceSubtitle = sheet.findViewById(R.id.tvExamSourceSubtitle);

        ChipGroup cgQuestionPool = sheet.findViewById(R.id.cgQuestionPool);
        Chip chipPoolSelected = sheet.findViewById(R.id.chipPoolSelected);
        Chip chipPoolAllCurrent = sheet.findViewById(R.id.chipPoolAllCurrent);
        Chip chipPool10 = sheet.findViewById(R.id.chipPool10);
        Chip chipPool20 = sheet.findViewById(R.id.chipPool20);
        Chip chipPool50 = sheet.findViewById(R.id.chipPool50);

        ChipGroup cgNegativeMarking = sheet.findViewById(R.id.cgNegativeMarking);
        Chip chipNegZero = sheet.findViewById(R.id.chipNegZero);
        Chip chipNeg020 = sheet.findViewById(R.id.chipNeg020);
        Chip chipNeg025 = sheet.findViewById(R.id.chipNeg025);
        Chip chipNeg050 = sheet.findViewById(R.id.chipNeg050);
        Chip chipNeg100 = sheet.findViewById(R.id.chipNeg100);

        ChipGroup cgDuration = sheet.findViewById(R.id.cgDuration);
        Chip chipDurationAuto = sheet.findViewById(R.id.chipDurationAuto);
        Chip chipDuration5 = sheet.findViewById(R.id.chipDuration5);
        Chip chipDuration10 = sheet.findViewById(R.id.chipDuration10);
        Chip chipDuration15 = sheet.findViewById(R.id.chipDuration15);
        Chip chipDuration20 = sheet.findViewById(R.id.chipDuration20);
        Chip chipDuration30 = sheet.findViewById(R.id.chipDuration30);
        Chip chipDurationUntimed = sheet.findViewById(R.id.chipDurationUntimed);

        SwitchCompat switchShuffle = sheet.findViewById(R.id.switchShuffle);
        SwitchCompat switchInstantFeedback = sheet.findViewById(R.id.switchInstantFeedback);
        Button btnStartSelfExam = sheet.findViewById(R.id.btnStartSelfExam);

        // Default Title
        if (!TextUtils.isEmpty(selectedSubjectName)) {
            etExamTitle.setText(selectedSubjectName + " Self-Test");
        } else {
            etExamTitle.setText("Self-Test Practice Exam");
        }

        // Setup Question Pool Chips
        int selectedCount = adapter.getSelectedCount();
        if (selectedCount > 0) {
            chipPoolSelected.setVisibility(View.VISIBLE);
            chipPoolSelected.setText("Selected (" + selectedCount + ")");
            chipPoolSelected.setChecked(true);
            tvExamSourceSubtitle.setText(selectedCount + " questions selected from bank");
        } else {
            chipPoolSelected.setVisibility(View.GONE);
            chipPoolAllCurrent.setChecked(true);
            chipPoolAllCurrent.setText("All Loaded (" + allQuestions.size() + ")");
            tvExamSourceSubtitle.setText("Using all " + allQuestions.size() + " loaded questions");
        }

        btnStartSelfExam.setOnClickListener(v -> {
            String title = etExamTitle.getText() != null ? etExamTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                title = "Self-Test Practice Exam";
            }

            // Determine question set
            List<Question> examQuestions;
            if (chipPoolSelected.isChecked() && selectedCount > 0) {
                examQuestions = new ArrayList<>(adapter.getSelectedQuestions());
            } else if (chipPool10.isChecked()) {
                examQuestions = pickSubList(allQuestions, 10);
            } else if (chipPool20.isChecked()) {
                examQuestions = pickSubList(allQuestions, 20);
            } else if (chipPool50.isChecked()) {
                examQuestions = pickSubList(allQuestions, 50);
            } else {
                examQuestions = new ArrayList<>(allQuestions);
            }

            if (examQuestions.isEmpty()) {
                Toast.makeText(requireContext(), "No questions selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Shuffle
            if (switchShuffle.isChecked()) {
                Collections.shuffle(examQuestions);
            }

            // Negative marking
            double negativeCutMark = 0.25;
            if (chipNegZero.isChecked()) negativeCutMark = 0.00;
            else if (chipNeg020.isChecked()) negativeCutMark = 0.20;
            else if (chipNeg025.isChecked()) negativeCutMark = 0.25;
            else if (chipNeg050.isChecked()) negativeCutMark = 0.50;
            else if (chipNeg100.isChecked()) negativeCutMark = 1.00;

            // Duration
            int durationMinutes = Math.max(5, examQuestions.size() * 1);
            if (chipDuration5.isChecked()) durationMinutes = 5;
            else if (chipDuration10.isChecked()) durationMinutes = 10;
            else if (chipDuration15.isChecked()) durationMinutes = 15;
            else if (chipDuration20.isChecked()) durationMinutes = 20;
            else if (chipDuration30.isChecked()) durationMinutes = 30;
            else if (chipDurationUntimed.isChecked()) durationMinutes = 180; // 3 hours untimed

            // Build session
            CustomExamGenerateResponse examSession = new CustomExamGenerateResponse();
            examSession.setExamId(System.currentTimeMillis() % 100000);
            examSession.setTitle(title + " (" + examQuestions.size() + " Q)");
            examSession.setTotalQuestions(examQuestions.size());
            examSession.setDurationMinutes(durationMinutes);
            examSession.setNegativeMarking(negativeCutMark);
            examSession.setQuestions(examQuestions);

            // Store in-memory session (Avoids Binder Transaction Too Large & NotSerializableException)
            ExamSessionManager.getInstance().setSession(examSession, true);

            Intent intent = new Intent(requireContext(), ExamArenaActivity.class);
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
    }

    private List<Question> pickSubList(List<Question> source, int count) {
        List<Question> copy = new ArrayList<>(source);
        Collections.shuffle(copy);
        if (copy.size() > count) {
            return new ArrayList<>(copy.subList(0, count));
        }
        return copy;
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
                    fallbackToOfflineQuestions();
                }
            }
        });
    }

    private void handleFetchError(Response<?> response) {
        String msg = NetworkErrorParser.parseError(response, "Unable to load questions.");
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        if (adapter.getItemCount() == 0) {
            fallbackToOfflineQuestions();
        }
    }

    private void fallbackToOfflineQuestions() {
        if (!isAdded()) return;
        questionRepository.searchQuestionsOffline(searchQuery != null ? searchQuery : "").observe(getViewLifecycleOwner(), cached -> {
            if (cached != null && !cached.isEmpty()) {
                adapter.setQuestions(cached);
                layoutEmptyState.setVisibility(View.GONE);
                rvQuestions.setVisibility(View.VISIBLE);
                totalQuestions = cached.size();
                updateSubtitleCounter();
                Toast.makeText(getContext(), "Showing " + cached.size() + " offline cached questions", Toast.LENGTH_SHORT).show();
            } else {
                layoutEmptyState.setVisibility(View.VISIBLE);
                rvQuestions.setVisibility(View.GONE);
            }
        });
    }

    private void updateSubtitleCounter() {
        // Redesigned compact top bar without subtitle text
    }

    private void resetAllFilters() {
        searchQuery = "";
        selectedDifficulty = null;
        selectedSubjectId = null;
        selectedSubjectName = null;
        selectedSort = "recent";

        etQuestionSearch.setText("");
        btnClearSearch.setVisibility(View.GONE);
        layoutSearchContainer.setVisibility(View.GONE);

        updateActiveFilterBadges();
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
