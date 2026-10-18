package com.questions.govt.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.questions.govt.R;
import com.questions.govt.data.model.ExamResult;
import com.questions.govt.ui.exam.ExamAnswerPreviewActivity;
import com.questions.govt.ui.exam.ExamResultActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for student's attended exams history tab.
 */
public class ProfileExamAdapter extends RecyclerView.Adapter<ProfileExamAdapter.ExamViewHolder> {

    private final List<ExamResult> items = new ArrayList<>();

    public void submitList(List<ExamResult> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_exam, parent, false);
        return new ExamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ExamViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvExamTitle;
        private final TextView tvExamStatusBadge;
        private final TextView tvExamScore;
        private final TextView tvExamAccuracy;
        private final TextView tvExamRank;
        private final TextView tvExamDate;
        private final MaterialButton btnReviewAnswers;

        public ExamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExamTitle = itemView.findViewById(R.id.tvExamTitle);
            tvExamStatusBadge = itemView.findViewById(R.id.tvExamStatusBadge);
            tvExamScore = itemView.findViewById(R.id.tvExamScore);
            tvExamAccuracy = itemView.findViewById(R.id.tvExamAccuracy);
            tvExamRank = itemView.findViewById(R.id.tvExamRank);
            tvExamDate = itemView.findViewById(R.id.tvExamDate);
            btnReviewAnswers = itemView.findViewById(R.id.btnReviewAnswers);
        }

        public void bind(ExamResult result) {
            Context context = itemView.getContext();

            tvExamTitle.setText(result.getTitle());
            tvExamScore.setText(String.format(Locale.US, "Score: %.2f", result.getFinalScore()));
            tvExamAccuracy.setText(String.format(Locale.US, "Accuracy: %.1f%%", result.getAccuracyPercentage()));
            int rank = result.getRank() > 0 ? result.getRank() : result.getGlobalRank();
            tvExamRank.setText(rank > 0 ? String.format(Locale.getDefault(), "Rank #%d", rank) : "Unranked");
            tvExamDate.setText(result.getSubmittedAt() != null ? result.getSubmittedAt() : (result.getCompletedAt() != null ? result.getCompletedAt() : ""));

            if (result.isPassed()) {
                tvExamStatusBadge.setText("PASSED");
                tvExamStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
                tvExamStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.success_light));
            } else {
                tvExamStatusBadge.setText("FAILED");
                tvExamStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error));
                tvExamStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.error_light));
            }

            View.OnClickListener openPreview = v -> {
                Intent intent = new Intent(context, ExamAnswerPreviewActivity.class);
                intent.putExtra(ExamAnswerPreviewActivity.EXTRA_EXAM_ID, result.getExamId());
                intent.putExtra(ExamResultActivity.EXTRA_EXAM_RESULT, result);
                context.startActivity(intent);
            };

            itemView.setOnClickListener(openPreview);
            btnReviewAnswers.setOnClickListener(openPreview);
        }
    }
}
