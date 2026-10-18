package com.questions.govt.ui.search;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.questions.govt.R;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying paginated question search results.
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.QuestionViewHolder> {

    private final List<Question> questionList = new ArrayList<>();
    private final OnQuestionClickListener listener;
    private final QuestionRepository questionRepository;

    public interface OnQuestionClickListener {
        void onQuestionClick(Question question);
    }

    public SearchResultAdapter(Context context, OnQuestionClickListener listener) {
        this.listener = listener;
        this.questionRepository = QuestionRepository.getInstance(context);
    }

    public void setData(List<Question> newQuestions) {
        questionList.clear();
        if (newQuestions != null) {
            questionList.addAll(newQuestions);
        }
        notifyDataSetChanged();
    }

    public void addPageData(List<Question> moreQuestions) {
        if (moreQuestions != null && !moreQuestions.isEmpty()) {
            int startPos = questionList.size();
            questionList.addAll(moreQuestions);
            notifyItemRangeInserted(startPos, moreQuestions.size());
        }
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question_search_result, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questionList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvQuestionText.setText(question.getQuestionText());

        // Subject Badge
        String subject = question.getSubjectTitle();
        holder.tvQuestionSubject.setText(!TextUtils.isEmpty(subject) ? subject : "General");

        // Difficulty Badge
        String difficulty = question.getDifficulty();
        holder.tvQuestionDifficulty.setText(difficulty != null ? difficulty.toUpperCase() : "MEDIUM");
        if ("easy".equalsIgnoreCase(difficulty)) {
            holder.tvQuestionDifficulty.setTextColor(ContextCompat.getColor(context, R.color.diff_easy));
        } else if ("hard".equalsIgnoreCase(difficulty)) {
            holder.tvQuestionDifficulty.setTextColor(ContextCompat.getColor(context, R.color.diff_hard));
        } else {
            holder.tvQuestionDifficulty.setTextColor(ContextCompat.getColor(context, R.color.diff_medium));
        }

        // Optional Question Image (.webp)
        if (!TextUtils.isEmpty(question.getImageUrl())) {
            holder.ivQuestionImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(question.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivQuestionImage);
        } else {
            holder.ivQuestionImage.setVisibility(View.GONE);
        }

        // Footer Telemetry Stats
        String statsText = question.getViewsCount() + " Views • " +
                question.getAttemptsCount() + " Attempts • " +
                question.getStarsCount() + " Stars";
        holder.tvQuestionStats.setText(statsText);

        if (!TextUtils.isEmpty(question.getCreatorUsername())) {
            holder.tvCreatorName.setText("by " + question.getCreatorUsername());
            holder.tvCreatorName.setVisibility(View.VISIBLE);
        } else {
            holder.tvCreatorName.setVisibility(View.GONE);
        }

        // Star Toggle
        bindStarIcon(holder.btnStarQuestion, question.isStarred(), context);
        holder.btnStarQuestion.setOnClickListener(v -> {
            questionRepository.toggleStar(question, new QuestionRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isStarred) {
                    bindStarIcon(holder.btnStarQuestion, isStarred, context);
                    holder.tvQuestionStats.setText(question.getViewsCount() + " Views • " +
                            question.getAttemptsCount() + " Attempts • " +
                            question.getStarsCount() + " Stars");
                }

                @Override
                public void onError(String error) {}
            });
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuestionClick(question);
            }
        });
    }

    private void bindStarIcon(ImageView imageView, boolean isStarred, Context context) {
        if (isStarred) {
            imageView.setColorFilter(ContextCompat.getColor(context, R.color.amber_accent));
        } else {
            imageView.setColorFilter(ContextCompat.getColor(context, R.color.text_muted));
        }
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionSubject;
        TextView tvQuestionDifficulty;
        ImageView btnStarQuestion;
        TextView tvQuestionText;
        ImageView ivQuestionImage;
        TextView tvQuestionStats;
        TextView tvCreatorName;

        QuestionViewHolder(View itemView) {
            super(itemView);
            tvQuestionSubject = itemView.findViewById(R.id.tvQuestionSubject);
            tvQuestionDifficulty = itemView.findViewById(R.id.tvQuestionDifficulty);
            btnStarQuestion = itemView.findViewById(R.id.btnStarQuestion);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            ivQuestionImage = itemView.findViewById(R.id.ivQuestionImage);
            tvQuestionStats = itemView.findViewById(R.id.tvQuestionStats);
            tvCreatorName = itemView.findViewById(R.id.tvCreatorName);
        }
    }
}
