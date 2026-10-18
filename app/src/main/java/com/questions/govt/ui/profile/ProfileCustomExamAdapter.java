package com.questions.govt.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.questions.govt.R;
import com.questions.govt.data.model.CustomExamRequest;
import com.questions.govt.ui.exam.ExamArenaActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for student's saved Custom Exam configurations.
 */
public class ProfileCustomExamAdapter extends RecyclerView.Adapter<ProfileCustomExamAdapter.CustomExamViewHolder> {

    private final List<CustomExamRequest> items = new ArrayList<>();

    public void submitList(List<CustomExamRequest> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CustomExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_custom_exam, parent, false);
        return new CustomExamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomExamViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class CustomExamViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvCustomExamTitle;
        private final TextView tvCustomExamConfig;
        private final MaterialButton btnRetakeExam;

        public CustomExamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomExamTitle = itemView.findViewById(R.id.tvCustomExamTitle);
            tvCustomExamConfig = itemView.findViewById(R.id.tvCustomExamConfig);
            btnRetakeExam = itemView.findViewById(R.id.btnRetakeExam);
        }

        public void bind(CustomExamRequest config) {
            Context context = itemView.getContext();

            tvCustomExamTitle.setText(config.getTitle());
            tvCustomExamConfig.setText(String.format(
                    Locale.US,
                    "%d Questions • %d Mins • Neg: %.2f • %s",
                    config.getTotalQuestions(),
                    config.getDurationMinutes(),
                    config.getNegativeMarking(),
                    config.getDifficulty()
            ));

            btnRetakeExam.setOnClickListener(v -> {
                Intent intent = new Intent(context, ExamArenaActivity.class);
                intent.putExtra(ExamArenaActivity.EXTRA_CUSTOM_REQUEST, config);
                context.startActivity(intent);
            });
        }
    }
}
