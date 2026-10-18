package com.govtexam.prep.ui.search;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.govtexam.prep.R;
import com.govtexam.prep.data.model.Question;
import com.govtexam.prep.data.remote.ApiClient;
import com.govtexam.prep.data.remote.response.ApiResponse;
import com.govtexam.prep.data.remote.response.PaginatedList;
import com.govtexam.prep.data.repository.QuestionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dedicated Search Results Activity with Sort Bar and server-side pagination.
 */
public class SearchResultActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY = "extra_query";
    public static final String EXTRA_SUBJECT_ID = "extra_subject_id";
    public static final String EXTRA_TOPIC_ID = "extra_topic_id";
    public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    public static final String EXTRA_TAG = "extra_tag";
    public static final String EXTRA_IS_STARRED = "extra_is_starred";

    private Toolbar toolbar;
    private ChipGroup cgSortOptions;
    private SwipeRefreshLayout swipeRefreshResults;
    private RecyclerView rvSearchResults;
    private LinearLayout layoutEmptyResults;
    private ProgressBar pbPagination;

    private SearchResultAdapter adapter;
    private QuestionRepository questionRepository;

    private String currentQuery = "";
    private long currentSubjectId = -1;
    private long currentTopicId = -1;
    private long currentCategoryId = -1;
    private String currentTag = "";
    private boolean isStarredOnly = false;
    private String currentSort = "relevance";

    private int currentPage = 1;
    private int lastPage = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        questionRepository = QuestionRepository.getInstance(this);

        extractIntentParameters();
        initViews();
        setupRecyclerViewAndPagination();
        setupSortBar();

        if (isStarredOnly) {
            loadStarredQuestionsOffline();
        } else {
            loadResults(1, true);
        }
    }

    private void extractIntentParameters() {
        if (getIntent().hasExtra(EXTRA_QUERY)) {
            currentQuery = getIntent().getStringExtra(EXTRA_QUERY);
        }
        currentSubjectId = getIntent().getLongExtra(EXTRA_SUBJECT_ID, -1);
        currentTopicId = getIntent().getLongExtra(EXTRA_TOPIC_ID, -1);
        currentCategoryId = getIntent().getLongExtra(EXTRA_CATEGORY_ID, -1);
        if (getIntent().hasExtra(EXTRA_TAG)) {
            currentTag = getIntent().getStringExtra(EXTRA_TAG);
        }
        isStarredOnly = getIntent().getBooleanExtra(EXTRA_IS_STARRED, false);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        cgSortOptions = findViewById(R.id.cgSortOptions);
        swipeRefreshResults = findViewById(R.id.swipeRefreshResults);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        layoutEmptyResults = findViewById(R.id.layoutEmptyResults);
        pbPagination = findViewById(R.id.pbPagination);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (isStarredOnly) {
                getSupportActionBar().setTitle("Saved Questions");
            } else if (!TextUtils.isEmpty(currentQuery)) {
                getSupportActionBar().setTitle("Search: " + currentQuery);
            } else if (!TextUtils.isEmpty(currentTag)) {
                getSupportActionBar().setTitle("Tag: #" + currentTag);
            } else {
                getSupportActionBar().setTitle("Browse Questions");
            }
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefreshResults.setOnRefreshListener(() -> {
            if (isStarredOnly) {
                loadStarredQuestionsOffline();
            } else {
                loadResults(1, true);
            }
        });
    }

    private void setupRecyclerViewAndPagination() {
        adapter = new SearchResultAdapter(this, question -> {
            // Record view telemetry
            questionRepository.recordQuestionView(question.getId());
            Toast.makeText(SearchResultActivity.this, "Question: " + question.getQuestionText(), Toast.LENGTH_SHORT).show();
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvSearchResults.setLayoutManager(layoutManager);
        rvSearchResults.setAdapter(adapter);

        rvSearchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isStarredOnly) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && currentPage < lastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4) {
                            loadResults(currentPage + 1, false);
                        }
                    }
                }
            }
        });
    }

    private void setupSortBar() {
        if (isStarredOnly) {
            cgSortOptions.setVisibility(View.GONE);
            return;
        }

        cgSortOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipSortNewest) {
                currentSort = "newest";
            } else if (checkedId == R.id.chipSortViews) {
                currentSort = "views";
            } else if (checkedId == R.id.chipSortStars) {
                currentSort = "stars";
            } else if (checkedId == R.id.chipSortEasyFirst) {
                currentSort = "diff_asc";
            } else if (checkedId == R.id.chipSortHardFirst) {
                currentSort = "diff_desc";
            } else {
                currentSort = "relevance";
            }
            loadResults(1, true);
        });
    }

    private void loadResults(int page, boolean isRefresh) {
        isLoading = true;
        if (isRefresh) {
            swipeRefreshResults.setRefreshing(true);
        } else {
            pbPagination.setVisibility(View.VISIBLE);
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("page", String.valueOf(page));
        filters.put("sort", currentSort);
        if (!TextUtils.isEmpty(currentQuery)) filters.put("q", currentQuery);
        if (currentSubjectId > 0) filters.put("subject_id", String.valueOf(currentSubjectId));
        if (currentTopicId > 0) filters.put("topic_id", String.valueOf(currentTopicId));
        if (currentCategoryId > 0) filters.put("category_id", String.valueOf(currentCategoryId));
        if (!TextUtils.isEmpty(currentTag)) filters.put("tag", currentTag);

        ApiClient.getApiService().searchQuestions(filters).enqueue(new Callback<ApiResponse<PaginatedList<Question>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedList<Question>>> call, Response<ApiResponse<PaginatedList<Question>>> response) {
                isLoading = false;
                swipeRefreshResults.setRefreshing(false);
                pbPagination.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    PaginatedList<Question> pageData = response.body().getData();
                    currentPage = pageData.getCurrentPage();
                    lastPage = pageData.getLastPage();

                    List<Question> questions = pageData.getData();
                    if (isRefresh) {
                        adapter.setData(questions);
                    } else {
                        adapter.addPageData(questions);
                    }

                    // Cache in Room for offline reading
                    questionRepository.cacheQuestions(questions);

                    layoutEmptyResults.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                } else {
                    fallbackToOfflineSearch();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PaginatedList<Question>>> call, Throwable t) {
                isLoading = false;
                swipeRefreshResults.setRefreshing(false);
                pbPagination.setVisibility(View.GONE);
                fallbackToOfflineSearch();
            }
        });
    }

    private void fallbackToOfflineSearch() {
        questionRepository.searchQuestionsOffline(currentQuery).observe(this, cachedList -> {
            if (cachedList != null && !cachedList.isEmpty()) {
                adapter.setData(cachedList);
                layoutEmptyResults.setVisibility(View.GONE);
                Toast.makeText(SearchResultActivity.this, "Serving cached questions (Offline)", Toast.LENGTH_SHORT).show();
            } else {
                layoutEmptyResults.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadStarredQuestionsOffline() {
        swipeRefreshResults.setRefreshing(false);
        questionRepository.getStarredQuestions().observe(this, starredQuestions -> {
            if (starredQuestions != null) {
                adapter.setData(starredQuestions);
                layoutEmptyResults.setVisibility(starredQuestions.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }
}
