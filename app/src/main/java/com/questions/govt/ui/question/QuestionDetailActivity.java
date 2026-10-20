package com.questions.govt.ui.question;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.MaterialToolbar;
import com.questions.govt.R;
import com.questions.govt.data.local.AppDatabase;
import com.questions.govt.data.model.Comment;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.ApiService;
import com.questions.govt.data.remote.response.AnswerCheckResponse;
import com.questions.govt.data.remote.response.ApiResponse;
import com.questions.govt.data.remote.response.VoteResponse;
import com.questions.govt.data.repository.QuestionRepository;
import com.questions.govt.util.LyxMathParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dedicated Single Question Detail Activity:
 * - Full Question Details with diagram images
 * - Adaptive compact poll-style options (Dynamic 2-column vs 1-column grid)
 * - Interactive answer checking with instant feedback
 * - Reliable Vote / Unvote toggle system with live state and counters
 * - Full Nested Commenting system (Top-level comments, replies, nested reply-to-reply)
 */
public class QuestionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTION_ID = "extra_question_id";

    private long questionId;
    private Question question;

    private MaterialToolbar toolbarQuestionDetail;
    private ImageView btnStarDetail;
    private ImageView btnReportDetail;

    private TextView tvSubjectBadgeDetail;
    private TextView tvDifficultyBadgeDetail;
    private TextView tvAuthorDetail;
    private TextView tvQuestionTextDetail;
    private ImageView ivQuestionImageDetail;

    private LinearLayout layoutRowABDetail;
    private LinearLayout layoutRowCDDetail;

    private LinearLayout layoutOptionADetail;
    private TextView tvLabelADetail;
    private TextView tvOptionADetail;
    private TextView tvPctADetail;
    private ProgressBar pbOptionADetail;

    private LinearLayout layoutOptionBDetail;
    private TextView tvLabelBDetail;
    private TextView tvOptionBDetail;
    private TextView tvPctBDetail;
    private ProgressBar pbOptionBDetail;

    private LinearLayout layoutOptionCDetail;
    private TextView tvLabelCDetail;
    private TextView tvOptionCDetail;
    private TextView tvPctCDetail;
    private ProgressBar pbOptionCDetail;

    private LinearLayout layoutOptionDDetail;
    private TextView tvLabelDDetail;
    private TextView tvOptionDDetail;
    private TextView tvPctDDetail;
    private ProgressBar pbOptionDDetail;

    private LinearLayout layoutExplanationDetail;
    private TextView tvCorrectAnswerHeaderDetail;
    private TextView tvExplanationTextDetail;

    // Voting
    private LinearLayout btnUpvoteDetail;
    private ImageView ivUpvoteDetail;
    private TextView tvUpvoteCountDetail;

    private LinearLayout btnDownvoteDetail;
    private ImageView ivDownvoteDetail;
    private TextView tvDownvoteCountDetail;

    private TextView tvTotalCommentsHeader;

    // Comments Section
    private RecyclerView rvDetailComments;
    private TextView tvEmptyComments;
    private ProgressBar pbCommentsLoading;

    private LinearLayout layoutReplyingToDetail;
    private TextView tvReplyingToDetail;
    private ImageView ivCancelReplyDetail;
    private EditText etCommentInputDetail;
    private AppCompatImageButton btnSendCommentDetail;

    private CommentAdapter commentAdapter;
    private final List<Comment> commentsList = new ArrayList<>();
    private Long replyParentId = null;

    private ApiService apiService;
    private QuestionRepository questionRepository;

    private String currentUserVote = null; // "up", "down", or null
    private int upvotesCount = 0;
    private int downvotesCount = 0;
    private boolean isAnswerChecked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        questionId = getIntent().getLongExtra(EXTRA_QUESTION_ID, -1);
        if (questionId <= 0) {
            Toast.makeText(this, "Invalid Question ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = ApiClient.getApiService();
        questionRepository = QuestionRepository.getInstance(this);

        initViews();
        setupListeners();
        setupCommentsRecyclerView();

        loadQuestionData();
        loadComments();

        // Record view telemetry
        questionRepository.recordQuestionView(questionId);
    }

    private void initViews() {
        toolbarQuestionDetail = findViewById(R.id.toolbarQuestionDetail);
        btnStarDetail = findViewById(R.id.btnStarDetail);
        btnReportDetail = findViewById(R.id.btnReportDetail);

        tvSubjectBadgeDetail = findViewById(R.id.tvSubjectBadgeDetail);
        tvDifficultyBadgeDetail = findViewById(R.id.tvDifficultyBadgeDetail);
        tvAuthorDetail = findViewById(R.id.tvAuthorDetail);
        tvQuestionTextDetail = findViewById(R.id.tvQuestionTextDetail);
        ivQuestionImageDetail = findViewById(R.id.ivQuestionImageDetail);

        layoutRowABDetail = findViewById(R.id.layoutRowABDetail);
        layoutRowCDDetail = findViewById(R.id.layoutRowCDDetail);

        layoutOptionADetail = findViewById(R.id.layoutOptionADetail);
        tvLabelADetail = findViewById(R.id.tvLabelADetail);
        tvOptionADetail = findViewById(R.id.tvOptionADetail);
        tvPctADetail = findViewById(R.id.tvPctADetail);
        pbOptionADetail = findViewById(R.id.pbOptionADetail);

        layoutOptionBDetail = findViewById(R.id.layoutOptionBDetail);
        tvLabelBDetail = findViewById(R.id.tvLabelBDetail);
        tvOptionBDetail = findViewById(R.id.tvOptionBDetail);
        tvPctBDetail = findViewById(R.id.tvPctBDetail);
        pbOptionBDetail = findViewById(R.id.pbOptionBDetail);

        layoutOptionCDetail = findViewById(R.id.layoutOptionCDetail);
        tvLabelCDetail = findViewById(R.id.tvLabelCDetail);
        tvOptionCDetail = findViewById(R.id.tvOptionCDetail);
        tvPctCDetail = findViewById(R.id.tvPctCDetail);
        pbOptionCDetail = findViewById(R.id.pbOptionCDetail);

        layoutOptionDDetail = findViewById(R.id.layoutOptionDDetail);
        tvLabelDDetail = findViewById(R.id.tvLabelDDetail);
        tvOptionDDetail = findViewById(R.id.tvOptionDDetail);
        tvPctDDetail = findViewById(R.id.tvPctDDetail);
        pbOptionDDetail = findViewById(R.id.pbOptionDDetail);

        layoutExplanationDetail = findViewById(R.id.layoutExplanationDetail);
        tvCorrectAnswerHeaderDetail = findViewById(R.id.tvCorrectAnswerHeaderDetail);
        tvExplanationTextDetail = findViewById(R.id.tvExplanationTextDetail);

        btnUpvoteDetail = findViewById(R.id.btnUpvoteDetail);
        ivUpvoteDetail = findViewById(R.id.ivUpvoteDetail);
        tvUpvoteCountDetail = findViewById(R.id.tvUpvoteCountDetail);

        btnDownvoteDetail = findViewById(R.id.btnDownvoteDetail);
        ivDownvoteDetail = findViewById(R.id.ivDownvoteDetail);
        tvDownvoteCountDetail = findViewById(R.id.tvDownvoteCountDetail);

        tvTotalCommentsHeader = findViewById(R.id.tvTotalCommentsHeader);

        rvDetailComments = findViewById(R.id.rvDetailComments);
        tvEmptyComments = findViewById(R.id.tvEmptyComments);
        pbCommentsLoading = findViewById(R.id.pbCommentsLoading);

        layoutReplyingToDetail = findViewById(R.id.layoutReplyingToDetail);
        tvReplyingToDetail = findViewById(R.id.tvReplyingToDetail);
        ivCancelReplyDetail = findViewById(R.id.ivCancelReplyDetail);
        etCommentInputDetail = findViewById(R.id.etCommentInputDetail);
        btnSendCommentDetail = findViewById(R.id.btnSendCommentDetail);

        toolbarQuestionDetail.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnStarDetail.setOnClickListener(v -> toggleStar());
        btnReportDetail.setOnClickListener(v -> {
            ReportDialogFragment dialog = ReportDialogFragment.newInstance(questionId);
            dialog.show(getSupportFragmentManager(), "report_question");
        });

        layoutOptionADetail.setOnClickListener(v -> onOptionSelected("A"));
        layoutOptionBDetail.setOnClickListener(v -> onOptionSelected("B"));
        layoutOptionCDetail.setOnClickListener(v -> onOptionSelected("C"));
        layoutOptionDDetail.setOnClickListener(v -> onOptionSelected("D"));

        btnUpvoteDetail.setOnClickListener(v -> onVoteClicked("up"));
        btnDownvoteDetail.setOnClickListener(v -> onVoteClicked("down"));

        ivCancelReplyDetail.setOnClickListener(v -> cancelReply());
        btnSendCommentDetail.setOnClickListener(v -> sendCommentOrReply());
    }

    private void setupCommentsRecyclerView() {
        commentAdapter = new CommentAdapter(commentsList, comment -> {
            replyParentId = comment.getId();
            layoutReplyingToDetail.setVisibility(View.VISIBLE);
            tvReplyingToDetail.setText("Replying to @" + comment.getUserUsername());
            etCommentInputDetail.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etCommentInputDetail, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        rvDetailComments.setLayoutManager(new LinearLayoutManager(this));
        rvDetailComments.setAdapter(commentAdapter);
    }

    private void loadQuestionData() {
        // First attempt offline cache
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Question cached = AppDatabase.getInstance(this).questionDao().getQuestionByIdSync(questionId);
            if (cached != null) {
                runOnUiThread(() -> bindQuestion(cached));
            }
        });

        // Fetch fresh question detail from network
        apiService.getQuestionDetail(questionId).enqueue(new Callback<ApiResponse<Question>>() {
            @Override
            public void onResponse(Call<ApiResponse<Question>> call, Response<ApiResponse<Question>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Question remote = response.body().getData();
                    bindQuestion(remote);
                    AppDatabase.databaseWriteExecutor.execute(() ->
                            AppDatabase.getInstance(QuestionDetailActivity.this).questionDao().insertOrReplace(remote));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Question>> call, Throwable t) {
                // If offline cache was bound, user can still study
            }
        });
    }

    private void bindQuestion(Question q) {
        this.question = q;

        tvSubjectBadgeDetail.setText(!TextUtils.isEmpty(q.getSubjectTitle()) ? q.getSubjectTitle() : "General");
        String diff = q.getDifficulty();
        tvDifficultyBadgeDetail.setText(diff != null ? diff.toUpperCase() : "MEDIUM");
        if ("easy".equalsIgnoreCase(diff)) {
            tvDifficultyBadgeDetail.setTextColor(ContextCompat.getColor(this, R.color.diff_easy));
        } else if ("hard".equalsIgnoreCase(diff)) {
            tvDifficultyBadgeDetail.setTextColor(ContextCompat.getColor(this, R.color.diff_hard));
        } else {
            tvDifficultyBadgeDetail.setTextColor(ContextCompat.getColor(this, R.color.diff_medium));
        }

        if (q.getAuthor() != null && !TextUtils.isEmpty(q.getAuthor().name)) {
            tvAuthorDetail.setText("By " + q.getAuthor().name);
        } else if (!TextUtils.isEmpty(q.getCreatorName())) {
            tvAuthorDetail.setText("By " + q.getCreatorName());
        } else {
            tvAuthorDetail.setText("");
        }

        LyxMathParser.setMathText(tvQuestionTextDetail, q.getQuestionText());

        if (!TextUtils.isEmpty(q.getImageUrl())) {
            ivQuestionImageDetail.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(q.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivQuestionImageDetail);
        } else {
            ivQuestionImageDetail.setVisibility(View.GONE);
        }

        LyxMathParser.setMathText(tvOptionADetail, q.getOptionA());
        LyxMathParser.setMathText(tvOptionBDetail, q.getOptionB());
        LyxMathParser.setMathText(tvOptionCDetail, q.getOptionC());
        LyxMathParser.setMathText(tvOptionDDetail, q.getOptionD());

        // Dynamic 2-column vs 1-column layout
        boolean isShort = isShortOption(q.getOptionA()) &&
                isShortOption(q.getOptionB()) &&
                isShortOption(q.getOptionC()) &&
                isShortOption(q.getOptionD());
        configureAdaptiveOptions(isShort);

        // Star state
        bindStarIcon(q.isStarred());

        // Voting state
        upvotesCount = q.getUpvotesCount();
        downvotesCount = q.getDownvotesCount();
        if (q.getUserInteractions() != null) {
            currentUserVote = q.getUserInteractions().getMyVote();
        }
        updateVotingUI();

        // Total comments counter
        tvTotalCommentsHeader.setText(q.getTotalComments() + " comments");
    }

    private boolean isShortOption(String text) {
        return text != null && text.trim().length() <= 24 && !text.contains("\n");
    }

    private void configureAdaptiveOptions(boolean isShort) {
        int margin4dp = (int) (4 * getResources().getDisplayMetrics().density);
        int margin6dp = (int) (6 * getResources().getDisplayMetrics().density);

        if (isShort) {
            layoutRowABDetail.setOrientation(LinearLayout.HORIZONTAL);
            layoutRowCDDetail.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lpA.setMarginEnd(margin4dp);
            layoutOptionADetail.setLayoutParams(lpA);

            LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lpB.setMarginStart(margin4dp);
            layoutOptionBDetail.setLayoutParams(lpB);

            LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lpC.setMarginEnd(margin4dp);
            layoutOptionCDetail.setLayoutParams(lpC);

            LinearLayout.LayoutParams lpD = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lpD.setMarginStart(margin4dp);
            layoutOptionDDetail.setLayoutParams(lpD);
        } else {
            layoutRowABDetail.setOrientation(LinearLayout.VERTICAL);
            layoutRowCDDetail.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutOptionADetail.setLayoutParams(lpA);

            LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpB.topMargin = margin6dp;
            layoutOptionBDetail.setLayoutParams(lpB);

            LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutOptionCDetail.setLayoutParams(lpC);

            LinearLayout.LayoutParams lpD = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpD.topMargin = margin6dp;
            layoutOptionDDetail.setLayoutParams(lpD);
        }
    }

    private void onOptionSelected(String selectedOption) {
        if (question == null) return;
        if (isAnswerChecked) {
            Toast.makeText(this, "You have already voted! You can vote again in 1 hour.", Toast.LENGTH_SHORT).show();
            return;
        }
        isAnswerChecked = true;

        String correctOpt = question.getCorrectOption() != null ? question.getCorrectOption().trim() : "A";

        // Highlight correct and wrong options
        highlightOption("A", correctOpt.equalsIgnoreCase("A"), selectedOption.equalsIgnoreCase("A"));
        highlightOption("B", correctOpt.equalsIgnoreCase("B"), selectedOption.equalsIgnoreCase("B"));
        highlightOption("C", correctOpt.equalsIgnoreCase("C"), selectedOption.equalsIgnoreCase("C"));
        highlightOption("D", correctOpt.equalsIgnoreCase("D"), selectedOption.equalsIgnoreCase("D"));

        // Reveal poll distribution percentages
        revealOptionPercentages();

        // Reveal explanation
        layoutExplanationDetail.setVisibility(View.VISIBLE);
        tvCorrectAnswerHeaderDetail.setText("Correct Answer: Option " + correctOpt);
        if (!TextUtils.isEmpty(question.getExplanation())) {
            LyxMathParser.setMathText(tvExplanationTextDetail, question.getExplanation());
        } else {
            tvExplanationTextDetail.setText("No additional explanation provided.");
        }

        // Send telemetry to backend and update community poll percentages
        Map<String, String> body = new HashMap<>();
        body.put("selected_option", selectedOption);
        apiService.checkAnswer(questionId, body).enqueue(new Callback<ApiResponse<AnswerCheckResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AnswerCheckResponse>> call, Response<ApiResponse<AnswerCheckResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AnswerCheckResponse data = response.body().getData();
                    question.setOptionAPct(data.getOptionAPct());
                    question.setOptionBPct(data.getOptionBPct());
                    question.setOptionCPct(data.getOptionCPct());
                    question.setOptionDPct(data.getOptionDPct());
                    if (!TextUtils.isEmpty(data.getExplanation())) {
                        question.setExplanation(data.getExplanation());
                        LyxMathParser.setMathText(tvExplanationTextDetail, data.getExplanation());
                    }
                    revealOptionPercentages();
                } else {
                    String error = com.questions.govt.util.NetworkErrorParser.parseError(response, "You can only vote once per hour.");
                    Toast.makeText(QuestionDetailActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AnswerCheckResponse>> call, Throwable t) {
                String error = com.questions.govt.util.NetworkErrorParser.parseFailure(t, "Network error checking answer.");
                Toast.makeText(QuestionDetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void highlightOption(String optionKey, boolean isCorrect, boolean isSelected) {
        LinearLayout layout;
        TextView tvLabel;
        switch (optionKey) {
            case "A": layout = layoutOptionADetail; tvLabel = tvLabelADetail; break;
            case "B": layout = layoutOptionBDetail; tvLabel = tvLabelBDetail; break;
            case "C": layout = layoutOptionCDetail; tvLabel = tvLabelCDetail; break;
            default: layout = layoutOptionDDetail; tvLabel = tvLabelDDetail; break;
        }

        if (isCorrect) {
            layout.setBackgroundResource(R.drawable.bg_option_correct);
            tvLabel.setBackgroundResource(R.drawable.bg_dialog_rounded);
            tvLabel.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green_correct));
            tvLabel.setTextColor(ContextCompat.getColor(this, R.color.text_white));
        } else if (isSelected) {
            layout.setBackgroundResource(R.drawable.bg_option_wrong);
            tvLabel.setBackgroundResource(R.drawable.bg_dialog_rounded);
            tvLabel.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red_wrong));
            tvLabel.setTextColor(ContextCompat.getColor(this, R.color.text_white));
        }
    }

    private void revealOptionPercentages() {
        if (question == null) return;
        tvPctADetail.setVisibility(View.VISIBLE);
        pbOptionADetail.setVisibility(View.VISIBLE);
        tvPctBDetail.setVisibility(View.VISIBLE);
        pbOptionBDetail.setVisibility(View.VISIBLE);
        tvPctCDetail.setVisibility(View.VISIBLE);
        pbOptionCDetail.setVisibility(View.VISIBLE);
        tvPctDDetail.setVisibility(View.VISIBLE);
        pbOptionDDetail.setVisibility(View.VISIBLE);

        int pctA = (int) question.getOptionAPct();
        int pctB = (int) question.getOptionBPct();
        int pctC = (int) question.getOptionCPct();
        int pctD = (int) question.getOptionDPct();

        tvPctADetail.setText(pctA + "%");
        pbOptionADetail.setProgress(pctA);

        tvPctBDetail.setText(pctB + "%");
        pbOptionBDetail.setProgress(pctB);

        tvPctCDetail.setText(pctC + "%");
        pbOptionCDetail.setProgress(pctC);

        tvPctDDetail.setText(pctD + "%");
        pbOptionDDetail.setProgress(pctD);
    }

    private void bindStarIcon(boolean isStarred) {
        btnStarDetail.setColorFilter(ContextCompat.getColor(this, isStarred ? R.color.amber_accent : R.color.text_muted));
    }

    private void toggleStar() {
        if (question == null) return;
        questionRepository.toggleStar(question, new QuestionRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isStarred) {
                question.setStarred(isStarred);
                bindStarIcon(isStarred);
                Toast.makeText(QuestionDetailActivity.this, isStarred ? "Question bookmarked" : "Removed bookmark", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(QuestionDetailActivity.this, "Could not update bookmark", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onVoteClicked(String targetType) {
        String actionType;
        if (targetType.equalsIgnoreCase(currentUserVote)) {
            // Unvote toggle
            actionType = "unvote";
            if ("up".equalsIgnoreCase(currentUserVote)) {
                upvotesCount = Math.max(0, upvotesCount - 1);
            } else if ("down".equalsIgnoreCase(currentUserVote)) {
                downvotesCount = Math.max(0, downvotesCount - 1);
            }
            currentUserVote = null;
        } else {
            // Cast new vote or switch vote
            actionType = targetType;
            if ("up".equalsIgnoreCase(targetType)) {
                upvotesCount++;
                if ("down".equalsIgnoreCase(currentUserVote)) {
                    downvotesCount = Math.max(0, downvotesCount - 1);
                }
                currentUserVote = "up";
            } else {
                downvotesCount++;
                if ("up".equalsIgnoreCase(currentUserVote)) {
                    upvotesCount = Math.max(0, upvotesCount - 1);
                }
                currentUserVote = "down";
            }
        }

        updateVotingUI();

        // Synchronize with server
        Map<String, Object> body = new HashMap<>();
        body.put("vote_type", actionType);
        apiService.voteQuestionAnswer(questionId, body).enqueue(new Callback<ApiResponse<VoteResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<VoteResponse>> call, Response<ApiResponse<VoteResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    VoteResponse vr = response.body().getData();
                    upvotesCount = vr.getUpvotesCount();
                    downvotesCount = vr.getDownvotesCount();
                    currentUserVote = vr.getUserVoted();
                    updateVotingUI();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VoteResponse>> call, Throwable t) {}
        });
    }

    private void updateVotingUI() {
        tvUpvoteCountDetail.setText(String.valueOf(upvotesCount));
        tvDownvoteCountDetail.setText(String.valueOf(downvotesCount));

        if ("up".equalsIgnoreCase(currentUserVote)) {
            ivUpvoteDetail.setColorFilter(ContextCompat.getColor(this, R.color.primary));
            tvUpvoteCountDetail.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvUpvoteCountDetail.setTypeface(null, Typeface.BOLD);

            ivDownvoteDetail.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
            tvDownvoteCountDetail.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvDownvoteCountDetail.setTypeface(null, Typeface.NORMAL);
        } else if ("down".equalsIgnoreCase(currentUserVote)) {
            ivDownvoteDetail.setColorFilter(ContextCompat.getColor(this, R.color.red_wrong));
            tvDownvoteCountDetail.setTextColor(ContextCompat.getColor(this, R.color.red_wrong));
            tvDownvoteCountDetail.setTypeface(null, Typeface.BOLD);

            ivUpvoteDetail.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
            tvUpvoteCountDetail.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvUpvoteCountDetail.setTypeface(null, Typeface.NORMAL);
        } else {
            ivUpvoteDetail.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
            tvUpvoteCountDetail.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvUpvoteCountDetail.setTypeface(null, Typeface.NORMAL);

            ivDownvoteDetail.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
            tvDownvoteCountDetail.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvDownvoteCountDetail.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void loadComments() {
        pbCommentsLoading.setVisibility(View.VISIBLE);
        apiService.getComments(questionId).enqueue(new Callback<ApiResponse<List<Comment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Comment>>> call, Response<ApiResponse<List<Comment>>> response) {
                pbCommentsLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    commentsList.clear();
                    commentsList.addAll(response.body().getData());
                    commentAdapter.notifyDataSetChanged();
                    tvEmptyComments.setVisibility(commentsList.isEmpty() ? View.VISIBLE : View.GONE);
                    tvTotalCommentsHeader.setText(commentsList.size() + " comments");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Comment>>> call, Throwable t) {
                pbCommentsLoading.setVisibility(View.GONE);
                tvEmptyComments.setVisibility(commentsList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void cancelReply() {
        replyParentId = null;
        layoutReplyingToDetail.setVisibility(View.GONE);
    }

    private void sendCommentOrReply() {
        String text = etCommentInputDetail.getText() != null ? etCommentInputDetail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        btnSendCommentDetail.setEnabled(false);
        Map<String, String> body = new HashMap<>();
        body.put("comment_text", text);

        Call<ApiResponse<Comment>> call;
        if (replyParentId != null && replyParentId > 0) {
            body.put("parent_id", String.valueOf(replyParentId));
            call = apiService.postReply(replyParentId, body);
        } else {
            call = apiService.postComment(questionId, body);
        }

        final Long submittedParentId = replyParentId;
        call.enqueue(new Callback<ApiResponse<Comment>>() {
            @Override
            public void onResponse(Call<ApiResponse<Comment>> c, Response<ApiResponse<Comment>> response) {
                btnSendCommentDetail.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Comment newComment = response.body().getData();
                    etCommentInputDetail.setText("");
                    cancelReply();

                    // If it was a reply, attach to parent comment replies list instantly
                    if (submittedParentId != null && submittedParentId > 0) {
                        boolean attached = false;
                        for (Comment parent : commentsList) {
                            if (parent.getId() == submittedParentId) {
                                if (parent.getReplies() == null) {
                                    parent.setReplies(new ArrayList<>());
                                }
                                parent.getReplies().add(newComment);
                                attached = true;
                                break;
                            }
                        }
                        if (!attached) {
                            commentsList.add(0, newComment);
                        }
                        commentAdapter.notifyDataSetChanged();
                    } else {
                        commentsList.add(0, newComment);
                        commentAdapter.notifyItemInserted(0);
                        rvDetailComments.scrollToPosition(0);
                    }

                    tvEmptyComments.setVisibility(View.GONE);
                    tvTotalCommentsHeader.setText(commentsList.size() + " comments");

                    Toast.makeText(QuestionDetailActivity.this, "Comment posted!", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = "Could not post comment.";
                    if (response.body() != null && !TextUtils.isEmpty(response.body().getMessage())) {
                        msg = response.body().getMessage();
                    }
                    Toast.makeText(QuestionDetailActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Comment>> c, Throwable t) {
                btnSendCommentDetail.setEnabled(true);
                Toast.makeText(QuestionDetailActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
