package com.questions.govt.ui.exam;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.questions.govt.R;
import com.questions.govt.data.model.CustomExamRequest;
import com.questions.govt.data.model.ExamResult;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.remote.response.CustomExamGenerateResponse;
import com.questions.govt.data.repository.ExamRepository;
import com.questions.govt.util.ExamSessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Live Exam Arena Activity with countdown timer, answer tracking,
 * question navigator palette, and offline queue resilience.
 */
public class ExamArenaActivity extends AppCompatActivity {

    public static final String EXTRA_EXAM_DATA = "extra_exam_data";
    public static final String EXTRA_EXAM_ID = "extra_exam_id";
    public static final String EXTRA_CUSTOM_REQUEST = "extra_custom_request";

    private TextView tvExamTitle;
    private TextView tvTimer;
    private ProgressBar pbExamProgress;
    private TextView tvQuestionCounter;
    private Button btnQuestionPalette;

    private TextView tvArenaQuestionText;
    private ImageView ivArenaQuestionImage;

    private LinearLayout layoutOptionA;
    private TextView tvOptionA;
    private LinearLayout layoutOptionB;
    private TextView tvOptionB;
    private LinearLayout layoutOptionC;
    private TextView tvOptionC;
    private LinearLayout layoutOptionD;
    private TextView tvOptionD;
    private Button btnClearAnswer;

    private Button btnPreviousQuestion;
    private Button btnNextQuestion;

    private long examId = 0;
    private String examTitle = "Exam";
    private int durationMinutes = 20;
    private final List<Question> questionList = new ArrayList<>();
    private final Map<Long, String> userAnswers = new HashMap<>();

    private int currentIndex = 0;
    private CountDownTimer countDownTimer;
    private long totalDurationMillis = 0;
    private long millisRemaining = 0;
    private boolean isSubmitted = false;

    private ExamRepository examRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_arena);

        examRepository = ExamRepository.getInstance(this);

        initViews();
        loadExamData();
    }

    private void initViews() {
        tvExamTitle = findViewById(R.id.tvExamTitle);
        tvTimer = findViewById(R.id.tvTimer);
        pbExamProgress = findViewById(R.id.pbExamProgress);
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter);
        btnQuestionPalette = findViewById(R.id.btnQuestionPalette);

        tvArenaQuestionText = findViewById(R.id.tvArenaQuestionText);
        ivArenaQuestionImage = findViewById(R.id.ivArenaQuestionImage);

        layoutOptionA = findViewById(R.id.layoutArenaOptionA);
        tvOptionA = findViewById(R.id.tvArenaOptionA);
        layoutOptionB = findViewById(R.id.layoutArenaOptionB);
        tvOptionB = findViewById(R.id.tvArenaOptionB);
        layoutOptionC = findViewById(R.id.layoutArenaOptionC);
        tvOptionC = findViewById(R.id.tvArenaOptionC);
        layoutOptionD = findViewById(R.id.layoutArenaOptionD);
        tvOptionD = findViewById(R.id.tvArenaOptionD);
        btnClearAnswer = findViewById(R.id.btnClearAnswer);

        btnPreviousQuestion = findViewById(R.id.btnPreviousQuestion);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        btnPreviousQuestion.setOnClickListener(v -> navigatePrevious());
        btnNextQuestion.setOnClickListener(v -> navigateNext());
        btnClearAnswer.setOnClickListener(v -> clearCurrentAnswer());
        btnQuestionPalette.setOnClickListener(v -> showQuestionPaletteDialog());

        // Option selections
        layoutOptionA.setOnClickListener(v -> selectOption("A"));
        layoutOptionB.setOnClickListener(v -> selectOption("B"));
        layoutOptionC.setOnClickListener(v -> selectOption("C"));
        layoutOptionD.setOnClickListener(v -> selectOption("D"));
    }

    private void loadExamData() {
        // 1. Check in-memory session (avoids Binder transaction limits & NotSerializableException)
        CustomExamGenerateResponse inMemorySession = ExamSessionManager.getInstance().getSession();
        if (inMemorySession != null) {
            examId = inMemorySession.getExamId();
            examTitle = inMemorySession.getTitle() != null ? inMemorySession.getTitle() : "Question Bank Exam";
            durationMinutes = inMemorySession.getDurationMinutes() > 0 ? inMemorySession.getDurationMinutes() : 20;
            if (inMemorySession.getQuestions() != null && !inMemorySession.getQuestions().isEmpty()) {
                questionList.clear();
                questionList.addAll(inMemorySession.getQuestions());
            }
            initArenaSession();
            return;
        }

        if (getIntent().hasExtra(EXTRA_CUSTOM_REQUEST)) {
            CustomExamRequest request = (CustomExamRequest) getIntent().getSerializableExtra(EXTRA_CUSTOM_REQUEST);
            if (request != null) {
                examTitle = request.getTitle();
                durationMinutes = request.getDurationMinutes();
                examRepository.generateCustomExam(request, new ExamRepository.ExamCallback<CustomExamGenerateResponse>() {
                    @Override
                    public void onSuccess(CustomExamGenerateResponse response) {
                        if (response != null) {
                            examId = response.getExamId();
                            if (response.getTitle() != null) examTitle = response.getTitle();
                            if (response.getDurationMinutes() > 0) durationMinutes = response.getDurationMinutes();
                            if (response.getQuestions() != null) {
                                questionList.clear();
                                questionList.addAll(response.getQuestions());
                            }
                            initArenaSession();
                        } else {
                            initArenaSession();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(ExamArenaActivity.this, message, Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
                return;
            }
        }

        if (getIntent().hasExtra(EXTRA_EXAM_DATA)) {
            CustomExamGenerateResponse data =
                    (CustomExamGenerateResponse) getIntent().getSerializableExtra(EXTRA_EXAM_DATA);
            if (data != null) {
                examId = data.getExamId();
                examTitle = data.getTitle();
                durationMinutes = data.getDurationMinutes();
                if (data.getQuestions() != null) {
                    questionList.addAll(data.getQuestions());
                }
            }
        }

        initArenaSession();
    }

    private void initArenaSession() {
        if (questionList.isEmpty()) {
            Toast.makeText(this, "No exam questions found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvExamTitle.setText(examTitle);
        pbExamProgress.setMax(questionList.size());
        totalDurationMillis = durationMinutes * 60 * 1000L;

        startTimer(totalDurationMillis);
        bindQuestion(0);
    }

    private void startTimer(long durationMillis) {
        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisRemaining = millisUntilFinished;
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(ExamArenaActivity.this, "Time is up! Submitting exam automatically.", Toast.LENGTH_LONG).show();
                finishExamSubmission();
            }
        }.start();
    }

    private void bindQuestion(int index) {
        if (index < 0 || index >= questionList.size()) return;
        currentIndex = index;
        Question question = questionList.get(index);

        tvQuestionCounter.setText("Question " + (index + 1) + " of " + questionList.size());
        pbExamProgress.setProgress(userAnswers.size());

        tvArenaQuestionText.setText(question.getQuestionText());
        tvOptionA.setText(question.getOptionA());
        tvOptionB.setText(question.getOptionB());
        tvOptionC.setText(question.getOptionC());
        tvOptionD.setText(question.getOptionD());

        if (!TextUtils.isEmpty(question.getImageUrl())) {
            ivArenaQuestionImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(question.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivArenaQuestionImage);
        } else {
            ivArenaQuestionImage.setVisibility(View.GONE);
        }

        // Highlight selected answer if already chosen
        String selected = userAnswers.get(question.getId());
        updateOptionStyles(selected);

        // Previous button state
        btnPreviousQuestion.setEnabled(index > 0);

        // Next or Finish button text
        if (index == questionList.size() - 1) {
            btnNextQuestion.setText("Finish Exam");
            btnNextQuestion.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.success));
        } else {
            btnNextQuestion.setText("Next");
            btnNextQuestion.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
        }
    }

    private void selectOption(String option) {
        Question current = questionList.get(currentIndex);
        userAnswers.put(current.getId(), option);
        updateOptionStyles(option);
        pbExamProgress.setProgress(userAnswers.size());
    }

    private void clearCurrentAnswer() {
        Question current = questionList.get(currentIndex);
        userAnswers.remove(current.getId());
        updateOptionStyles(null);
        pbExamProgress.setProgress(userAnswers.size());
    }

    private void updateOptionStyles(String selectedOption) {
        setOptionStyle(layoutOptionA, "A".equalsIgnoreCase(selectedOption));
        setOptionStyle(layoutOptionB, "B".equalsIgnoreCase(selectedOption));
        setOptionStyle(layoutOptionC, "C".equalsIgnoreCase(selectedOption));
        setOptionStyle(layoutOptionD, "D".equalsIgnoreCase(selectedOption));
    }

    private void setOptionStyle(LinearLayout layout, boolean isSelected) {
        if (isSelected) {
            layout.setBackgroundResource(R.drawable.bg_option_correct);
        } else {
            layout.setBackgroundResource(R.drawable.bg_option_card);
        }
    }

    private void navigatePrevious() {
        if (currentIndex > 0) {
            bindQuestion(currentIndex - 1);
        }
    }

    private void navigateNext() {
        if (currentIndex < questionList.size() - 1) {
            bindQuestion(currentIndex + 1);
        } else {
            confirmFinishExam();
        }
    }

    private void confirmFinishExam() {
        int unanswered = questionList.size() - userAnswers.size();
        String message = unanswered > 0
                ? "You have " + unanswered + " unanswered questions. Are you sure you want to finish the exam?"
                : "Are you ready to submit your exam?";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Submit Exam")
                .setMessage(message)
                .setPositiveButton("Submit Now", (dialog, which) -> finishExamSubmission())
                .setNegativeButton("Continue Exam", null)
                .show();
    }

    private void finishExamSubmission() {
        if (isSubmitted) return;
        isSubmitted = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        long timeSpentSeconds = (totalDurationMillis - millisRemaining) / 1000;
        if (timeSpentSeconds <= 0) timeSpentSeconds = 1;

        // If Self-Test from Question Bank, calculate scores locally with negative cut marks
        if (ExamSessionManager.getInstance().isSelfTest()) {
            CustomExamGenerateResponse session = ExamSessionManager.getInstance().getSession();
            double negativeMarking = session != null ? session.getNegativeMarking() : 0.25;

            int total = questionList.size();
            int correct = 0;
            int wrong = 0;
            int unattempted = 0;

            for (Question q : questionList) {
                String chosen = userAnswers.get(q.getId());
                String answer = q.getCorrectOption();
                if (TextUtils.isEmpty(chosen)) {
                    unattempted++;
                } else if (chosen.equalsIgnoreCase(answer)) {
                    correct++;
                } else {
                    wrong++;
                }
            }

            double negativeDeduction = wrong * Math.abs(negativeMarking);
            double finalScore = Math.max(0, (correct * 1.0) - negativeDeduction);
            float accuracy = total > 0 ? ((float) correct / total) * 100f : 0f;
            boolean isPassed = accuracy >= 50f;

            ExamResult result = new ExamResult();
            result.setId(examId > 0 ? examId : System.currentTimeMillis() % 100000);
            result.setExamId(result.getId());
            result.setTitle(examTitle);
            result.setTotalQuestions(total);
            result.setAttemptedCount(correct + wrong);
            result.setCorrectCount(correct);
            result.setWrongCount(wrong);
            result.setSkippedCount(unattempted);
            result.setRawScore(correct * 1.0);
            result.setNegativeDeduction(negativeDeduction);
            result.setFinalScore(finalScore);
            result.setAccuracyPercentage(accuracy);
            result.setTimeSpentSeconds(timeSpentSeconds);
            result.setPassed(isPassed);
            result.setPointsEarned(correct * 5.0);
            result.setCompletedAt(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));

            // Cache result in Room database
            com.questions.govt.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
                com.questions.govt.data.local.AppDatabase.getInstance(ExamArenaActivity.this).examDao().insertExamResult(result);
            });

            Intent intent = new Intent(ExamArenaActivity.this, ExamResultActivity.class);
            intent.putExtra(ExamResultActivity.EXTRA_RESULT, result);
            startActivity(intent);
            finish();
            return;
        }

        examRepository.submitExam(examId, examTitle, userAnswers, timeSpentSeconds, new ExamRepository.ExamCallback<ExamResult>() {
            @Override
            public void onSuccess(ExamResult result) {
                Intent intent = new Intent(ExamArenaActivity.this, ExamResultActivity.class);
                intent.putExtra(ExamResultActivity.EXTRA_RESULT, result);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ExamArenaActivity.this, "Submission completed.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showQuestionPaletteDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_question_palette, null);
        RecyclerView rvPalette = dialogView.findViewById(R.id.rvPaletteGrid);
        Button btnClose = dialogView.findViewById(R.id.btnClosePalette);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        rvPalette.setLayoutManager(new GridLayoutManager(this, 5));
        PaletteAdapter adapter = new PaletteAdapter(questionList, userAnswers, currentIndex, position -> {
            dialog.dismiss();
            bindQuestion(position);
        });
        rvPalette.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        confirmFinishExam();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // Question Palette Adapter
    private static class PaletteAdapter extends RecyclerView.Adapter<PaletteAdapter.ViewHolder> {
        private final List<Question> questions;
        private final Map<Long, String> answers;
        private final int activeIndex;
        private final OnPaletteClickListener listener;

        interface OnPaletteClickListener {
            void onPaletteClick(int position);
        }

        PaletteAdapter(List<Question> questions, Map<Long, String> answers, int activeIndex, OnPaletteClickListener listener) {
            this.questions = questions;
            this.answers = answers;
            this.activeIndex = activeIndex;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_palette_number, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Question q = questions.get(position);
            holder.tvNumber.setText(String.valueOf(position + 1));
            Context context = holder.itemView.getContext();

            boolean isAnswered = answers.containsKey(q.getId());
            boolean isCurrent = (position == activeIndex);

            if (isAnswered) {
                holder.tvNumber.setBackgroundResource(R.drawable.bg_option_correct);
                holder.tvNumber.setTextColor(ContextCompat.getColor(context, R.color.success));
            } else if (isCurrent) {
                holder.tvNumber.setBackgroundResource(R.drawable.bg_option_wrong);
                holder.tvNumber.setTextColor(ContextCompat.getColor(context, R.color.primary));
            } else {
                holder.tvNumber.setBackgroundResource(R.drawable.bg_option_card);
                holder.tvNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPaletteClick(position);
            });
        }

        @Override
        public int getItemCount() {
            return questions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNumber;
            ViewHolder(View itemView) {
                super(itemView);
                tvNumber = itemView.findViewById(R.id.tvPaletteNumber);
            }
        }
    }
}
