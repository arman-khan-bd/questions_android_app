package com.questions.govt.ui.exam;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.questions.govt.R;
import com.questions.govt.data.model.OfficialExamItem;

import java.util.ArrayList;
import java.util.List;

public class OfficialExamAdapter extends RecyclerView.Adapter<OfficialExamAdapter.ViewHolder> {

    public interface OnExamClickListener {
        void onExamClick(OfficialExamItem exam);
    }

    private final List<OfficialExamItem> exams = new ArrayList<>();
    private final OnExamClickListener listener;

    public OfficialExamAdapter(OnExamClickListener listener) {
        this.listener = listener;
    }

    public void setExams(List<OfficialExamItem> newExams) {
        exams.clear();
        if (newExams != null) {
            exams.addAll(newExams);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_official_exam, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OfficialExamItem exam = exams.get(position);
        holder.bind(exam, listener);
    }

    @Override
    public int getItemCount() {
        return exams.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvExamCodeBadge;
        private final TextView tvExamSubjectBadge;
        private final TextView tvRankedBadge;
        private final TextView tvExamTitle;
        private final TextView tvExamDescription;
        private final TextView tvExamQuestionsCount;
        private final TextView tvExamDuration;
        private final TextView tvExamNegativeCut;
        private final TextView tvUsersAttendedCount;
        private final TextView tvCorrectAnswersCount;
        private final TextView tvAccuracyBadge;
        private final MaterialButton btnAttendExam;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExamCodeBadge = itemView.findViewById(R.id.tvExamCodeBadge);
            tvExamSubjectBadge = itemView.findViewById(R.id.tvExamSubjectBadge);
            tvRankedBadge = itemView.findViewById(R.id.tvRankedBadge);
            tvExamTitle = itemView.findViewById(R.id.tvExamTitle);
            tvExamDescription = itemView.findViewById(R.id.tvExamDescription);
            tvExamQuestionsCount = itemView.findViewById(R.id.tvExamQuestionsCount);
            tvExamDuration = itemView.findViewById(R.id.tvExamDuration);
            tvExamNegativeCut = itemView.findViewById(R.id.tvExamNegativeCut);
            tvUsersAttendedCount = itemView.findViewById(R.id.tvUsersAttendedCount);
            tvCorrectAnswersCount = itemView.findViewById(R.id.tvCorrectAnswersCount);
            tvAccuracyBadge = itemView.findViewById(R.id.tvAccuracyBadge);
            btnAttendExam = itemView.findViewById(R.id.btnAttendExam);
        }

        public void bind(OfficialExamItem exam, OnExamClickListener listener) {
            Context context = itemView.getContext();

            tvExamCodeBadge.setText(exam.getExamCode());
            tvExamTitle.setText(exam.getTitle());

            if (!TextUtils.isEmpty(exam.getDescription())) {
                tvExamDescription.setVisibility(View.VISIBLE);
                tvExamDescription.setText(exam.getDescription());
            } else {
                tvExamDescription.setVisibility(View.GONE);
            }

            if (exam.getSubject() != null && !TextUtils.isEmpty(exam.getSubject().getTitle())) {
                tvExamSubjectBadge.setVisibility(View.VISIBLE);
                tvExamSubjectBadge.setText(exam.getSubject().getTitle());
                if (!TextUtils.isEmpty(exam.getSubject().getColor())) {
                    try {
                        int color = Color.parseColor(exam.getSubject().getColor());
                        tvExamSubjectBadge.setTextColor(color);
                        tvExamSubjectBadge.setBackgroundTintList(ColorStateList.valueOf(Color.argb(35, Color.red(color), Color.green(color), Color.blue(color))));
                    } catch (Exception ignored) {}
                }
            } else {
                tvExamSubjectBadge.setVisibility(View.GONE);
            }

            tvExamQuestionsCount.setText(exam.getTotalQuestions() + " Questions");
            tvExamDuration.setText(exam.getDurationMinutes() + " mins");
            tvExamNegativeCut.setText("-" + exam.getNegativeMarking() + " Cut");

            tvUsersAttendedCount.setText(String.valueOf(exam.getTotalAttended()));
            tvCorrectAnswersCount.setText(String.valueOf(exam.getTotalCorrect()));

            if (exam.getTotalAttended() > 0 && exam.getAccuracyPercentage() > 0) {
                tvAccuracyBadge.setText("Accuracy: " + exam.getAccuracyPercentage() + "%");
                tvAccuracyBadge.setVisibility(View.VISIBLE);
            } else {
                tvAccuracyBadge.setText("Open for all aspirants");
                tvAccuracyBadge.setVisibility(View.VISIBLE);
            }

            View.OnClickListener clickHandler = v -> {
                if (listener != null) {
                    listener.onExamClick(exam);
                }
            };

            btnAttendExam.setOnClickListener(clickHandler);
            itemView.setOnClickListener(clickHandler);
        }
    }
}
