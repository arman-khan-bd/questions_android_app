package com.questions.govt.ui.exam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.questions.govt.R;
import com.questions.govt.data.model.ExamResult;
import com.questions.govt.ui.main.MainActivity;

import java.util.Locale;

/**
 * Exam Results Screen showing final score with negative marking breakdown,
 * pass/fail status, accuracy %, time spent, and updated global rank.
 */
public class ExamResultActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT = "extra_exam_result";
    public static final String EXTRA_EXAM_RESULT = EXTRA_RESULT;

    private TextView tvPassFailBadge;
    private TextView tvFinalScore;
    private TextView tvRawScore;
    private TextView tvNegativeDeduction;
    private TextView tvCorrectCount;
    private TextView tvWrongCount;
    private TextView tvSkippedCount;
    private TextView tvAccuracyPct;
    private TextView tvTimeSpent;
    private TextView tvResultRank;
    private TextView tvResultPoints;
    private Button btnPreviewAnswers;
    private Button btnBackHome;

    private ExamResult examResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_result);

        if (getIntent().hasExtra(EXTRA_RESULT)) {
            examResult = (ExamResult) getIntent().getSerializableExtra(EXTRA_RESULT);
        }

        initViews();
        bindData();
    }

    private void initViews() {
        tvPassFailBadge = findViewById(R.id.tvPassFailBadge);
        tvFinalScore = findViewById(R.id.tvFinalScore);
        tvRawScore = findViewById(R.id.tvRawScore);
        tvNegativeDeduction = findViewById(R.id.tvNegativeDeduction);
        tvCorrectCount = findViewById(R.id.tvCorrectCount);
        tvWrongCount = findViewById(R.id.tvWrongCount);
        tvSkippedCount = findViewById(R.id.tvSkippedCount);
        tvAccuracyPct = findViewById(R.id.tvAccuracyPct);
        tvTimeSpent = findViewById(R.id.tvTimeSpent);
        tvResultRank = findViewById(R.id.tvResultRank);
        tvResultPoints = findViewById(R.id.tvResultPoints);
        btnPreviewAnswers = findViewById(R.id.btnPreviewAnswers);
        btnBackHome = findViewById(R.id.btnBackHome);

        btnPreviewAnswers.setOnClickListener(v -> {
            if (examResult != null) {
                if (examResult.getAnswerSheet() != null && !examResult.getAnswerSheet().isEmpty()) {
                    com.questions.govt.util.ExamSessionManager.getInstance().setLastPreviewList(examResult.getAnswerSheet());
                }
                Intent intent = new Intent(ExamResultActivity.this, ExamAnswerPreviewActivity.class);
                intent.putExtra(ExamAnswerPreviewActivity.EXTRA_EXAM_ID, examResult.getExamId());
                intent.putExtra(ExamAnswerPreviewActivity.EXTRA_ATTEMPT_ID, examResult.getAttemptId());

                // Create a lightweight copy for Intent to prevent TransactionTooLargeException
                ExamResult lightResult = new ExamResult();
                lightResult.setId(examResult.getId());
                lightResult.setExamId(examResult.getExamId());
                lightResult.setAttemptId(examResult.getAttemptId());
                lightResult.setTitle(examResult.getTitle());
                lightResult.setExamType(examResult.getExamType());
                lightResult.setAffectsRanking(examResult.isAffectsRanking());
                lightResult.setFinalScore(examResult.getFinalScore());
                lightResult.setTotalQuestions(examResult.getTotalQuestions());
                lightResult.setCorrectCount(examResult.getCorrectCount());
                lightResult.setWrongCount(examResult.getWrongCount());
                lightResult.setSkippedCount(examResult.getSkippedCount());
                intent.putExtra(ExamAnswerPreviewActivity.EXTRA_EXAM_RESULT, lightResult);

                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Intent fallback = new Intent(ExamResultActivity.this, ExamAnswerPreviewActivity.class);
                    fallback.putExtra(ExamAnswerPreviewActivity.EXTRA_EXAM_ID, examResult.getExamId());
                    startActivity(fallback);
                }
            }
        });

        btnBackHome.setOnClickListener(v -> navigateBackHome());
    }

    @Override
    public void onBackPressed() {
        navigateBackHome();
    }

    private void navigateBackHome() {
        Intent intent = new Intent(ExamResultActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SHOW_SPLASH_LOADING, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void bindData() {
        if (examResult == null) return;

        // Pass / Fail Badge
        if (examResult.isPassed()) {
            tvPassFailBadge.setText("PASSED");
            tvPassFailBadge.setBackgroundResource(R.drawable.bg_option_correct);
            tvPassFailBadge.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            tvPassFailBadge.setText("FAILED");
            tvPassFailBadge.setBackgroundResource(R.drawable.bg_option_wrong);
            tvPassFailBadge.setTextColor(ContextCompat.getColor(this, R.color.error));
        }

        // Scores
        tvFinalScore.setText(String.format(Locale.getDefault(), "%.2f", examResult.getFinalScore()));
        tvRawScore.setText(String.format(Locale.getDefault(), "Marks: +%.2f", examResult.getRawScore()));
        tvNegativeDeduction.setText(String.format(Locale.getDefault(), "Penalty: -%.2f", examResult.getNegativeDeduction()));

        // Counts
        tvCorrectCount.setText(String.valueOf(examResult.getCorrectCount()));
        tvWrongCount.setText(String.valueOf(examResult.getWrongCount()));
        tvSkippedCount.setText(String.valueOf(examResult.getSkippedCount()));

        // Accuracy & Time
        tvAccuracyPct.setText(String.format(Locale.getDefault(), "Accuracy: %.1f%%", examResult.getAccuracyPercentage()));
        long minutes = examResult.getTimeSpentSeconds() / 60;
        long seconds = examResult.getTimeSpentSeconds() % 60;
        tvTimeSpent.setText(String.format(Locale.getDefault(), "Time: %02dm %02ds", minutes, seconds));

        // Rank & Points
        if (!examResult.isAffectsRanking() || "practice".equalsIgnoreCase(examResult.getExamType()) || "custom".equalsIgnoreCase(examResult.getExamType())) {
            tvResultRank.setText("Practice (Unranked)");
            tvResultPoints.setText("Practice Mode (No Rank Impact)");
        } else {
            tvResultRank.setText(examResult.getGlobalRank() > 0 ? "#" + examResult.getGlobalRank() : "Unranked");
            tvResultPoints.setText(String.format(Locale.getDefault(), "+%.0f pts", examResult.getPointsEarned()));
        }
    }
}
