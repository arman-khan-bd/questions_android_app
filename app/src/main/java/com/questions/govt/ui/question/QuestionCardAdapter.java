package com.questions.govt.ui.question;

import android.content.Context;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.User;
import com.questions.govt.data.remote.response.AnswerCheckResponse;
import com.questions.govt.data.remote.response.VoteResponse;
import com.questions.govt.data.repository.QuestionRepository;
import com.questions.govt.util.FeatureFlagManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Question Card Adapter with instant answer checking, creator direct reveal,
 * rank-neutral accuracy voting, star toggling, comments sheet, and report dialog.
 */
public class QuestionCardAdapter extends RecyclerView.Adapter<QuestionCardAdapter.CardViewHolder> {

    private final List<Question> questionList = new ArrayList<>();
    private final QuestionRepository questionRepository;
    private final FragmentManager fragmentManager;
    private final User currentUser;

    public QuestionCardAdapter() {
        this(GovtExamApp.getInstance(), null);
    }

    public QuestionCardAdapter(Context context, FragmentManager fragmentManager) {
        this.questionRepository = QuestionRepository.getInstance(context);
        this.fragmentManager = fragmentManager;
        this.currentUser = SessionManager.getInstance().getUser();
    }

    public void submitList(List<Question> questions) {
        setQuestions(questions);
    }

    public void setQuestions(List<Question> questions) {
        questionList.clear();
        if (questions != null) {
            questionList.addAll(questions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Question question = questionList.get(position);
        Context context = holder.itemView.getContext();

        // 1. Telemetry Impression Tracking
        questionRepository.recordQuestionView(question.getId());

        // 2. Metadata Badges
        holder.tvSubjectBadge.setText(!TextUtils.isEmpty(question.getSubjectTitle()) ? question.getSubjectTitle() : "General");
        String diff = question.getDifficulty();
        holder.tvDifficultyBadge.setText(diff != null ? diff.toUpperCase() : "MEDIUM");
        if ("easy".equalsIgnoreCase(diff)) {
            holder.tvDifficultyBadge.setTextColor(ContextCompat.getColor(context, R.color.diff_easy));
        } else if ("hard".equalsIgnoreCase(diff)) {
            holder.tvDifficultyBadge.setTextColor(ContextCompat.getColor(context, R.color.diff_hard));
        } else {
            holder.tvDifficultyBadge.setTextColor(ContextCompat.getColor(context, R.color.diff_medium));
        }

        // 3. Question Text and Image
        holder.tvQuestionText.setText(question.getQuestionText());
        if (!TextUtils.isEmpty(question.getImageUrl())) {
            holder.ivQuestionImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(question.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivQuestionImage);
        } else {
            holder.ivQuestionImage.setVisibility(View.GONE);
        }

        // 4. Option Texts
        holder.tvOptionA.setText(question.getOptionA());
        holder.tvOptionB.setText(question.getOptionB());
        holder.tvOptionC.setText(question.getOptionC());
        holder.tvOptionD.setText(question.getOptionD());

        // 5. Star Icon
        bindStar(holder.btnStar, question.isStarred(), context);
        holder.btnStar.setOnClickListener(v -> {
            questionRepository.toggleStar(question, new QuestionRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isStarred) {
                    bindStar(holder.btnStar, isStarred, context);
                }
                @Override
                public void onError(String error) {}
            });
        });

        // 6. Question Creator Direct Reveal Button
        if (currentUser != null && currentUser.getId() == question.getCreatorId()) {
            holder.btnCreatorReveal.setVisibility(View.VISIBLE);
            holder.btnCreatorReveal.setOnClickListener(v -> revealCreatorAnswer(holder, question, context));
        } else {
            holder.btnCreatorReveal.setVisibility(View.GONE);
        }

        // 7. Option Click Handling (Instant Answer Check)
        setupOptionClick(holder.layoutOptionA, "A", holder, question, context);
        setupOptionClick(holder.layoutOptionB, "B", holder, question, context);
        setupOptionClick(holder.layoutOptionC, "C", holder, question, context);
        setupOptionClick(holder.layoutOptionD, "D", holder, question, context);

        // If answer already revealed in state, re-bind visual states
        if (question.isAnswerRevealed()) {
            applyAnswerStyles(holder, question, context, question.getUserSelectedOption(), question.getCorrectOption());
        } else {
            resetOptionStyles(holder, context);
        }

        // 8. Voting System (Rank-Neutral)
        FeatureFlagManager flagManager = FeatureFlagManager.getInstance();
        holder.layoutVoting.setVisibility(flagManager.isAnswerVotingSystemEnabled() ? View.VISIBLE : View.GONE);
        holder.tvUpvotes.setText(String.valueOf(question.getUpvotesCount()));
        holder.tvDownvotes.setText(String.valueOf(question.getDownvotesCount()));

        holder.btnUpvote.setOnClickListener(v -> {
            questionRepository.voteAnswer(question.getId(), "up", new QuestionRepository.RepositoryCallback<VoteResponse>() {
                @Override
                public void onSuccess(VoteResponse result) {
                    holder.tvUpvotes.setText(String.valueOf(result.getUpvotesCount()));
                    holder.tvDownvotes.setText(String.valueOf(result.getDownvotesCount()));
                    Toast.makeText(context, "Vote recorded (Rank-Neutral)", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(String error) {}
            });
        });

        holder.btnDownvote.setOnClickListener(v -> {
            questionRepository.voteAnswer(question.getId(), "down", new QuestionRepository.RepositoryCallback<VoteResponse>() {
                @Override
                public void onSuccess(VoteResponse result) {
                    holder.tvUpvotes.setText(String.valueOf(result.getUpvotesCount()));
                    holder.tvDownvotes.setText(String.valueOf(result.getDownvotesCount()));
                    Toast.makeText(context, "Dispute vote recorded", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(String error) {}
            });
        });

        // 9. Comments & Discussions Bottom Sheet
        boolean commentsEnabled = flagManager.isQuestionCommentsEnabled();
        holder.btnComments.setVisibility(commentsEnabled ? View.VISIBLE : View.GONE);
        holder.tvCommentCount.setText(String.valueOf(question.getCommentsCount()));
        holder.btnComments.setOnClickListener(v -> {
            FragmentManager fm = fragmentManager;
            if (fm == null && context instanceof AppCompatActivity) {
                fm = ((AppCompatActivity) context).getSupportFragmentManager();
            }
            if (fm != null) {
                CommentBottomSheetFragment sheet = CommentBottomSheetFragment.newInstance(question.getId());
                sheet.show(fm, "CommentBottomSheet");
            }
        });

        // 10. Report Dialog
        holder.btnReport.setOnClickListener(v -> {
            FragmentManager fm = fragmentManager;
            if (fm == null && context instanceof AppCompatActivity) {
                fm = ((AppCompatActivity) context).getSupportFragmentManager();
            }
            if (fm != null) {
                ReportDialogFragment dialog = ReportDialogFragment.newInstance(question.getId());
                dialog.show(fm, "ReportDialog");
            }
        });
    }

    private void setupOptionClick(View optionView, String optionLetter, CardViewHolder holder, Question question, Context context) {
        optionView.setOnClickListener(v -> {
            if (question.isAnswerRevealed()) return; // Prevent multiple submissions

            question.setUserSelectedOption(optionLetter);
            questionRepository.checkAnswer(question.getId(), optionLetter, new QuestionRepository.RepositoryCallback<AnswerCheckResponse>() {
                @Override
                public void onSuccess(AnswerCheckResponse result) {
                    question.setAnswerRevealed(true);
                    question.setCorrectOption(result.getCorrectOption());
                    question.setExplanation(result.getExplanation());
                    question.setExplanationImageUrl(result.getExplanationImageUrl());
                    question.setOptionAPct(result.getOptionAPct());
                    question.setOptionBPct(result.getOptionBPct());
                    question.setOptionCPct(result.getOptionCPct());
                    question.setOptionDPct(result.getOptionDPct());

                    // If user was wrong, auto-add to local Mistake Notebook
                    if (!result.isCorrect()) {
                        questionRepository.setMistakeStatus(question.getId(), true);
                    }

                    applyAnswerStyles(holder, question, context, optionLetter, result.getCorrectOption());
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void revealCreatorAnswer(CardViewHolder holder, Question question, Context context) {
        question.setAnswerRevealed(true);
        applyAnswerStyles(holder, question, context, null, question.getCorrectOption());
    }

    private void applyAnswerStyles(CardViewHolder holder, Question question, Context context, String selectedOption, String correctOption) {
        // Highlight correct and wrong options
        highlightOption(holder.layoutOptionA, "A", selectedOption, correctOption, context);
        highlightOption(holder.layoutOptionB, "B", selectedOption, correctOption, context);
        highlightOption(holder.layoutOptionC, "C", selectedOption, correctOption, context);
        highlightOption(holder.layoutOptionD, "D", selectedOption, correctOption, context);

        // Display community selection distribution percentage bars
        showDistributionBar(holder.pbOptionA, holder.tvPctA, question.getOptionAPct());
        showDistributionBar(holder.pbOptionB, holder.tvPctB, question.getOptionBPct());
        showDistributionBar(holder.pbOptionC, holder.tvPctC, question.getOptionCPct());
        showDistributionBar(holder.pbOptionD, holder.tvPctD, question.getOptionDPct());

        // Reveal Explanation Card
        holder.layoutExplanation.setVisibility(View.VISIBLE);
        holder.tvCorrectAnswerHeader.setText("Correct Answer: Option " + correctOption);
        holder.tvExplanationText.setText(question.getExplanation());

        if (!TextUtils.isEmpty(question.getExplanationImageUrl())) {
            holder.ivExplanationImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(question.getExplanationImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivExplanationImage);
        } else {
            holder.ivExplanationImage.setVisibility(View.GONE);
        }
    }

    private void highlightOption(LinearLayout layout, String letter, String selectedOption, String correctOption, Context context) {
        if (letter.equalsIgnoreCase(correctOption)) {
            layout.setBackgroundResource(R.drawable.bg_option_correct);
        } else if (letter.equalsIgnoreCase(selectedOption)) {
            layout.setBackgroundResource(R.drawable.bg_option_wrong);
        } else {
            layout.setBackgroundResource(R.drawable.bg_option_card);
        }
    }

    private void resetOptionStyles(CardViewHolder holder, Context context) {
        holder.layoutOptionA.setBackgroundResource(R.drawable.bg_option_card);
        holder.layoutOptionB.setBackgroundResource(R.drawable.bg_option_card);
        holder.layoutOptionC.setBackgroundResource(R.drawable.bg_option_card);
        holder.layoutOptionD.setBackgroundResource(R.drawable.bg_option_card);

        holder.pbOptionA.setVisibility(View.GONE);
        holder.pbOptionB.setVisibility(View.GONE);
        holder.pbOptionC.setVisibility(View.GONE);
        holder.pbOptionD.setVisibility(View.GONE);

        holder.tvPctA.setVisibility(View.GONE);
        holder.tvPctB.setVisibility(View.GONE);
        holder.tvPctC.setVisibility(View.GONE);
        holder.tvPctD.setVisibility(View.GONE);

        holder.layoutExplanation.setVisibility(View.GONE);
    }

    private void showDistributionBar(ProgressBar pb, TextView tvPct, float percentage) {
        pb.setVisibility(View.VISIBLE);
        tvPct.setVisibility(View.VISIBLE);
        int intPct = Math.round(percentage);
        pb.setProgress(intPct);
        tvPct.setText(intPct + "%");
    }

    private void bindStar(ImageView ivStar, boolean isStarred, Context context) {
        ivStar.setColorFilter(ContextCompat.getColor(context, isStarred ? R.color.amber_accent : R.color.text_muted));
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectBadge;
        TextView tvDifficultyBadge;
        Button btnCreatorReveal;
        ImageView btnStar;
        TextView tvQuestionText;
        ImageView ivQuestionImage;

        LinearLayout layoutOptionA;
        TextView tvOptionA;
        ProgressBar pbOptionA;
        TextView tvPctA;

        LinearLayout layoutOptionB;
        TextView tvOptionB;
        ProgressBar pbOptionB;
        TextView tvPctB;

        LinearLayout layoutOptionC;
        TextView tvOptionC;
        ProgressBar pbOptionC;
        TextView tvPctC;

        LinearLayout layoutOptionD;
        TextView tvOptionD;
        ProgressBar pbOptionD;
        TextView tvPctD;

        LinearLayout layoutExplanation;
        TextView tvCorrectAnswerHeader;
        TextView tvExplanationText;
        ImageView ivExplanationImage;

        LinearLayout layoutVoting;
        ImageView btnUpvote;
        TextView tvUpvotes;
        ImageView btnDownvote;
        TextView tvDownvotes;

        LinearLayout btnComments;
        TextView tvCommentCount;
        ImageView btnReport;

        CardViewHolder(View itemView) {
            super(itemView);
            tvSubjectBadge = itemView.findViewById(R.id.tvSubjectBadge);
            tvDifficultyBadge = itemView.findViewById(R.id.tvDifficultyBadge);
            btnCreatorReveal = itemView.findViewById(R.id.btnCreatorReveal);
            btnStar = itemView.findViewById(R.id.btnStar);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            ivQuestionImage = itemView.findViewById(R.id.ivQuestionImage);

            layoutOptionA = itemView.findViewById(R.id.layoutOptionA);
            tvOptionA = itemView.findViewById(R.id.tvOptionA);
            pbOptionA = itemView.findViewById(R.id.pbOptionA);
            tvPctA = itemView.findViewById(R.id.tvPctA);

            layoutOptionB = itemView.findViewById(R.id.layoutOptionB);
            tvOptionB = itemView.findViewById(R.id.tvOptionB);
            pbOptionB = itemView.findViewById(R.id.pbOptionB);
            tvPctB = itemView.findViewById(R.id.tvPctB);

            layoutOptionC = itemView.findViewById(R.id.layoutOptionC);
            tvOptionC = itemView.findViewById(R.id.tvOptionC);
            pbOptionC = itemView.findViewById(R.id.pbOptionC);
            tvPctC = itemView.findViewById(R.id.tvPctC);

            layoutOptionD = itemView.findViewById(R.id.layoutOptionD);
            tvOptionD = itemView.findViewById(R.id.tvOptionD);
            pbOptionD = itemView.findViewById(R.id.pbOptionD);
            tvPctD = itemView.findViewById(R.id.tvPctD);

            layoutExplanation = itemView.findViewById(R.id.layoutExplanation);
            tvCorrectAnswerHeader = itemView.findViewById(R.id.tvCorrectAnswerHeader);
            tvExplanationText = itemView.findViewById(R.id.tvExplanationText);
            ivExplanationImage = itemView.findViewById(R.id.ivExplanationImage);

            layoutVoting = itemView.findViewById(R.id.layoutVoting);
            btnUpvote = itemView.findViewById(R.id.btnUpvote);
            tvUpvotes = itemView.findViewById(R.id.tvUpvotes);
            btnDownvote = itemView.findViewById(R.id.btnDownvote);
            tvDownvotes = itemView.findViewById(R.id.tvDownvotes);

            btnComments = itemView.findViewById(R.id.btnComments);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            btnReport = itemView.findViewById(R.id.btnReport);
        }
    }
}
