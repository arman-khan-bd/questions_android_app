package com.questions.govt.ui.exam;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.questions.govt.R;
import com.questions.govt.data.model.ExamQuestionPreview;
import com.questions.govt.data.model.ExamResult;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.repository.ExamRepository;
import com.questions.govt.data.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Exam Answers & Explanations Preview Screen.
 * Displays user's selected answers, green correct keys, community percentages, and explanations.
 */
public class ExamAnswerPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_EXAM_ID = "extra_exam_id";
    public static final String EXTRA_ATTEMPT_ID = "extra_attempt_id";
    public static final String EXTRA_EXAM_RESULT = "extra_exam_result";

    private Toolbar toolbar;
    private ProgressBar pbPreviewLoading;
    private RecyclerView rvAnswersPreview;

    private ExamRepository examRepository;
    private QuestionRepository questionRepository;
    private PreviewAdapter adapter;
    private final List<ExamQuestionPreview> previewList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_answer_preview);

        examRepository = ExamRepository.getInstance(this);
        questionRepository = QuestionRepository.getInstance(this);

        initViews();
        loadAnswersPreview();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        pbPreviewLoading = findViewById(R.id.pbPreviewLoading);
        rvAnswersPreview = findViewById(R.id.rvAnswersPreview);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new PreviewAdapter(previewList, questionRepository);
        rvAnswersPreview.setLayoutManager(new LinearLayoutManager(this));
        rvAnswersPreview.setAdapter(adapter);
    }

    private void loadAnswersPreview() {
        // 1. Check in-memory preview list first (Instant, zero-network lag)
        List<ExamQuestionPreview> inMemory = com.questions.govt.util.ExamSessionManager.getInstance().getLastPreviewList();
        if (inMemory != null && !inMemory.isEmpty()) {
            previewList.clear();
            previewList.addAll(inMemory);
            adapter.notifyDataSetChanged();
            pbPreviewLoading.setVisibility(View.GONE);
            return;
        }

        // 2. Check ExamResult passed via Intent
        if (getIntent().hasExtra(EXTRA_EXAM_RESULT)) {
            ExamResult res = (ExamResult) getIntent().getSerializableExtra(EXTRA_EXAM_RESULT);
            if (res != null && res.getAnswerSheet() != null && !res.getAnswerSheet().isEmpty()) {
                previewList.clear();
                previewList.addAll(res.getAnswerSheet());
                adapter.notifyDataSetChanged();
                pbPreviewLoading.setVisibility(View.GONE);
                return;
            }
        }

        // 3. Fallback: Network fetch for past exams (e.g. from Profile -> Exams)
        long examId = getIntent().getLongExtra(EXTRA_EXAM_ID, 0);
        long attemptId = getIntent().getLongExtra(EXTRA_ATTEMPT_ID, 0);
        if (examId == 0 && getIntent().hasExtra(EXTRA_EXAM_RESULT)) {
            ExamResult res = (ExamResult) getIntent().getSerializableExtra(EXTRA_EXAM_RESULT);
            if (res != null) {
                examId = res.getExamId();
                if (attemptId == 0) attemptId = res.getAttemptId();
            }
        }

        long targetId = attemptId > 0 ? attemptId : examId;
        if (targetId == 0) {
            Toast.makeText(this, "Exam not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pbPreviewLoading.setVisibility(View.VISIBLE);
        examRepository.getExamAnswerPreview(targetId, new ExamRepository.ExamCallback<List<ExamQuestionPreview>>() {
            @Override
            public void onSuccess(List<ExamQuestionPreview> data) {
                pbPreviewLoading.setVisibility(View.GONE);
                if (data != null && !data.isEmpty()) {
                    previewList.clear();
                    previewList.addAll(data);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(ExamAnswerPreviewActivity.this, "No preview answers available.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                pbPreviewLoading.setVisibility(View.GONE);
                Toast.makeText(ExamAnswerPreviewActivity.this, message != null ? message : "Failed to load answer preview.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Preview Adapter
    private static class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.ViewHolder> {
        private final List<ExamQuestionPreview> items;
        private final QuestionRepository questionRepository;

        PreviewAdapter(List<ExamQuestionPreview> items, QuestionRepository questionRepository) {
            this.items = items;
            this.questionRepository = questionRepository;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_exam_answer_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ExamQuestionPreview item = items.get(position);
            Context context = holder.itemView.getContext();

            holder.tvQuestionNum.setText("Question " + (position + 1));
            holder.tvQuestionText.setText(item.getQuestionText());

            // Outcome badge
            String userSelected = item.getUserSelectedOption();
            String correct = item.getCorrectOption();
            boolean isSkipped = TextUtils.isEmpty(userSelected);
            boolean isCorrect = !isSkipped && userSelected.equalsIgnoreCase(correct);

            if (isSkipped) {
                holder.tvOutcomeBadge.setText("Skipped (0.00)");
                holder.tvOutcomeBadge.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                holder.tvOutcomeBadge.setBackgroundResource(R.drawable.bg_option_card);
            } else if (isCorrect) {
                holder.tvOutcomeBadge.setText("Correct (+1.00)");
                holder.tvOutcomeBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
                holder.tvOutcomeBadge.setBackgroundResource(R.drawable.bg_option_correct);
            } else {
                holder.tvOutcomeBadge.setText("Wrong Answer");
                holder.tvOutcomeBadge.setTextColor(ContextCompat.getColor(context, R.color.error));
                holder.tvOutcomeBadge.setBackgroundResource(R.drawable.bg_option_wrong);
            }

            // Question Image
            if (!TextUtils.isEmpty(item.getImageUrl())) {
                holder.ivQuestionImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(item.getImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.ivQuestionImage);
            } else {
                holder.ivQuestionImage.setVisibility(View.GONE);
            }

            // Options Text & Percentages
            holder.tvOptionA.setText(item.getOptionA());
            holder.tvOptionB.setText(item.getOptionB());
            holder.tvOptionC.setText(item.getOptionC());
            holder.tvOptionD.setText(item.getOptionD());

            holder.tvPctA.setText(Math.round(item.getOptionAPct()) + "%");
            holder.pbPctA.setProgress(Math.round(item.getOptionAPct()));
            holder.tvPctB.setText(Math.round(item.getOptionBPct()) + "%");
            holder.pbPctB.setProgress(Math.round(item.getOptionBPct()));
            holder.tvPctC.setText(Math.round(item.getOptionCPct()) + "%");
            holder.pbPctC.setProgress(Math.round(item.getOptionCPct()));
            holder.tvPctD.setText(Math.round(item.getOptionDPct()) + "%");
            holder.pbPctD.setProgress(Math.round(item.getOptionDPct()));

            // Highlight Correct key (Green) & User's wrong key (Red)
            bindOptionBackground(holder.layoutOptionA, "A", userSelected, correct);
            bindOptionBackground(holder.layoutOptionB, "B", userSelected, correct);
            bindOptionBackground(holder.layoutOptionC, "C", userSelected, correct);
            bindOptionBackground(holder.layoutOptionD, "D", userSelected, correct);

            // Explanation
            holder.tvCorrectKeyHeader.setText("Correct Answer: Option " + correct);
            holder.tvExplanation.setText(item.getExplanation());
            if (!TextUtils.isEmpty(item.getExplanationImageUrl())) {
                holder.ivExplanationImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(item.getExplanationImageUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.ivExplanationImage);
            } else {
                holder.ivExplanationImage.setVisibility(View.GONE);
            }

            // Star toggle
            bindStar(holder.btnStar, item.isStarred(), context);
            holder.btnStar.setOnClickListener(v -> {
                Question syntheticQuestion = new Question();
                syntheticQuestion.setId(item.getQuestionId());
                syntheticQuestion.setStarred(item.isStarred());

                questionRepository.toggleStar(syntheticQuestion, new QuestionRepository.RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isStarred) {
                        item.setStarred(isStarred);
                        bindStar(holder.btnStar, isStarred, context);
                    }
                    @Override
                    public void onError(String error) {}
                });
            });
        }

        private void bindOptionBackground(LinearLayout layout, String letter, String userSelected, String correctKey) {
            if (letter.equalsIgnoreCase(correctKey)) {
                layout.setBackgroundResource(R.drawable.bg_option_correct);
            } else if (letter.equalsIgnoreCase(userSelected)) {
                layout.setBackgroundResource(R.drawable.bg_option_wrong);
            } else {
                layout.setBackgroundResource(R.drawable.bg_option_card);
            }
        }

        private void bindStar(ImageView ivStar, boolean isStarred, Context context) {
            ivStar.setColorFilter(ContextCompat.getColor(context, isStarred ? R.color.amber_accent : R.color.text_muted));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvQuestionNum;
            TextView tvOutcomeBadge;
            ImageView btnStar;
            TextView tvQuestionText;
            ImageView ivQuestionImage;

            LinearLayout layoutOptionA;
            TextView tvOptionA;
            TextView tvPctA;
            ProgressBar pbPctA;

            LinearLayout layoutOptionB;
            TextView tvOptionB;
            TextView tvPctB;
            ProgressBar pbPctB;

            LinearLayout layoutOptionC;
            TextView tvOptionC;
            TextView tvPctC;
            ProgressBar pbPctC;

            LinearLayout layoutOptionD;
            TextView tvOptionD;
            TextView tvPctD;
            ProgressBar pbPctD;

            TextView tvCorrectKeyHeader;
            TextView tvExplanation;
            ImageView ivExplanationImage;

            ViewHolder(View itemView) {
                super(itemView);
                tvQuestionNum = itemView.findViewById(R.id.tvPreviewQuestionNum);
                tvOutcomeBadge = itemView.findViewById(R.id.tvUserOutcomeBadge);
                btnStar = itemView.findViewById(R.id.btnStarPreview);
                tvQuestionText = itemView.findViewById(R.id.tvPreviewQuestionText);
                ivQuestionImage = itemView.findViewById(R.id.ivPreviewQuestionImage);

                layoutOptionA = itemView.findViewById(R.id.layoutOptionA);
                tvOptionA = itemView.findViewById(R.id.tvPreviewOptionA);
                tvPctA = itemView.findViewById(R.id.tvPreviewPctA);
                pbPctA = itemView.findViewById(R.id.pbPreviewA);

                layoutOptionB = itemView.findViewById(R.id.layoutOptionB);
                tvOptionB = itemView.findViewById(R.id.tvPreviewOptionB);
                tvPctB = itemView.findViewById(R.id.tvPreviewPctB);
                pbPctB = itemView.findViewById(R.id.pbPreviewB);

                layoutOptionC = itemView.findViewById(R.id.layoutOptionC);
                tvOptionC = itemView.findViewById(R.id.tvPreviewOptionC);
                tvPctC = itemView.findViewById(R.id.tvPreviewPctC);
                pbPctC = itemView.findViewById(R.id.pbPreviewC);

                layoutOptionD = itemView.findViewById(R.id.layoutOptionD);
                tvOptionD = itemView.findViewById(R.id.tvPreviewOptionD);
                tvPctD = itemView.findViewById(R.id.tvPreviewPctD);
                pbPctD = itemView.findViewById(R.id.pbPreviewD);

                tvCorrectKeyHeader = itemView.findViewById(R.id.tvPreviewCorrectKeyHeader);
                tvExplanation = itemView.findViewById(R.id.tvPreviewExplanation);
                ivExplanationImage = itemView.findViewById(R.id.ivPreviewExplanationImage);
            }
        }
    }
}
