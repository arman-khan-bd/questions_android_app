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

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.questions.govt.GovtExamApp;
import com.questions.govt.R;
import com.questions.govt.data.local.SessionManager;
import com.questions.govt.data.model.Question;
import com.questions.govt.data.model.User;
import com.questions.govt.data.remote.response.AnswerCheckResponse;
import com.questions.govt.data.remote.response.VoteResponse;
import com.questions.govt.data.repository.QuestionRepository;
import com.questions.govt.util.FeatureFlagManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interactive Question Card Adapter with instant answer checking, creator direct reveal,
 * rank-neutral accuracy voting, star toggling, comments sheet, and report dialog.
 */
public class QuestionCardAdapter extends RecyclerView.Adapter<QuestionCardAdapter.CardViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count, List<Question> selectedQuestions);
    }

    private final List<Question> questionList = new ArrayList<>();
    private final Set<Long> selectedQuestionIds = new HashSet<>();
    private OnSelectionChangedListener onSelectionChangedListener;
    private final QuestionRepository questionRepository;
    private final FragmentManager fragmentManager;
    private final User currentUser;

    public QuestionCardAdapter() {
        this(GovtExamApp.getInstance(), null);
    }

    public QuestionCardAdapter(Context context, FragmentManager fragmentManager) {
        Context appContext = context != null ? context : GovtExamApp.getInstance();
        this.questionRepository = QuestionRepository.getInstance(appContext);
        this.fragmentManager = fragmentManager;
        this.currentUser = SessionManager.getInstance().getUser();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.onSelectionChangedListener = listener;
    }

    public List<Question> getSelectedQuestions() {
        List<Question> list = new ArrayList<>();
        for (Question q : questionList) {
            if (q != null && selectedQuestionIds.contains(q.getId())) {
                list.add(q);
            }
        }
        return list;
    }

    public int getSelectedCount() {
        return selectedQuestionIds.size();
    }

    public void selectAll() {
        for (Question q : questionList) {
            if (q != null) {
                selectedQuestionIds.add(q.getId());
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void clearSelection() {
        selectedQuestionIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void toggleSelection(Question question) {
        if (question == null) return;
        if (selectedQuestionIds.contains(question.getId())) {
            selectedQuestionIds.remove(question.getId());
        } else {
            selectedQuestionIds.add(question.getId());
        }
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedQuestionIds.size(), getSelectedQuestions());
        }
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

    public void addQuestions(List<Question> moreQuestions) {
        if (moreQuestions != null && !moreQuestions.isEmpty()) {
            int startPos = questionList.size();
            questionList.addAll(moreQuestions);
            notifyItemRangeInserted(startPos, moreQuestions.size());
        }
    }

    public List<Question> getQuestions() {
        return new ArrayList<>(questionList);
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

        // 1.1 Selection Checkbox & Long Click
        if (holder.cbSelectQuestion != null) {
            holder.cbSelectQuestion.setChecked(selectedQuestionIds.contains(question.getId()));
            holder.cbSelectQuestion.setOnClickListener(v -> toggleSelection(question));
        }
        holder.itemView.setOnLongClickListener(v -> {
            if (holder.cbSelectQuestion != null) {
                holder.cbSelectQuestion.setChecked(!holder.cbSelectQuestion.isChecked());
            }
            toggleSelection(question);
            return true;
        });

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

        // 2.1 Previous Submitted Answer Badge (preserves past attempt indicator without locking the card)
        String prevAnswer = question.getUserSelectedOption();
        if (!TextUtils.isEmpty(prevAnswer) && holder.tvPrevAnswerBadge != null) {
            holder.tvPrevAnswerBadge.setVisibility(View.VISIBLE);
            holder.tvPrevAnswerBadge.setText("Prev: Opt " + prevAnswer.trim().toUpperCase());
        } else if (holder.tvPrevAnswerBadge != null) {
            holder.tvPrevAnswerBadge.setVisibility(View.GONE);
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

        // 4. Option Texts & Adaptive Layout
        holder.tvOptionA.setText(question.getOptionA());
        holder.tvOptionB.setText(question.getOptionB());
        holder.tvOptionC.setText(question.getOptionC());
        holder.tvOptionD.setText(question.getOptionD());

        boolean isShort = isShortOption(question.getOptionA()) &&
                isShortOption(question.getOptionB()) &&
                isShortOption(question.getOptionC()) &&
                isShortOption(question.getOptionD());
        configureAdaptiveOptions(holder, isShort, context);

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

        // 8. Voting System (Rank-Neutral & Liked State)
        FeatureFlagManager flagManager = FeatureFlagManager.getInstance();
        holder.layoutVoting.setVisibility(flagManager.isAnswerVotingSystemEnabled() ? View.VISIBLE : View.GONE);
        bindVoting(holder, question, context);

        View.OnClickListener upvoteListener = v -> {
            String currentVote = question.getUserVoted();
            String voteAction = "up".equalsIgnoreCase(currentVote) ? "unvote" : "up";
            questionRepository.voteAnswer(question.getId(), voteAction, new QuestionRepository.RepositoryCallback<VoteResponse>() {
                @Override
                public void onSuccess(VoteResponse result) {
                    question.setUpvotesCount(result.getUpvotesCount());
                    question.setDownvotesCount(result.getDownvotesCount());
                    question.setUserVoted(result.getUserVoted());
                    bindVoting(holder, question, context);
                    String msg = "up".equalsIgnoreCase(result.getUserVoted()) ? "Upvoted!" : "Vote removed.";
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(context, error != null ? error : "Vote failed.", Toast.LENGTH_SHORT).show();
                }
            });
        };
        if (holder.layoutBtnUpvote != null) holder.layoutBtnUpvote.setOnClickListener(upvoteListener);
        holder.btnUpvote.setOnClickListener(upvoteListener);
        holder.tvUpvotes.setOnClickListener(upvoteListener);

        View.OnClickListener downvoteListener = v -> {
            String currentVote = question.getUserVoted();
            String voteAction = "down".equalsIgnoreCase(currentVote) ? "unvote" : "down";
            questionRepository.voteAnswer(question.getId(), voteAction, new QuestionRepository.RepositoryCallback<VoteResponse>() {
                @Override
                public void onSuccess(VoteResponse result) {
                    question.setUpvotesCount(result.getUpvotesCount());
                    question.setDownvotesCount(result.getDownvotesCount());
                    question.setUserVoted(result.getUserVoted());
                    bindVoting(holder, question, context);
                    String msg = "down".equalsIgnoreCase(result.getUserVoted()) ? "Dispute recorded!" : "Vote removed.";
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(context, error != null ? error : "Vote failed.", Toast.LENGTH_SHORT).show();
                }
            });
        };
        if (holder.layoutBtnDownvote != null) holder.layoutBtnDownvote.setOnClickListener(downvoteListener);
        holder.btnDownvote.setOnClickListener(downvoteListener);
        holder.tvDownvotes.setOnClickListener(downvoteListener);

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
            if (question.isAnswerRevealed()) {
                Toast.makeText(context, "You have already voted! You can vote again in 1 hour.", Toast.LENGTH_SHORT).show();
                return;
            }

            question.setUserSelectedOption(optionLetter);
            if (holder.tvPrevAnswerBadge != null) {
                holder.tvPrevAnswerBadge.setVisibility(View.VISIBLE);
                holder.tvPrevAnswerBadge.setText("Voted: Opt " + optionLetter);
            }
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
                    Toast.makeText(context, error != null ? error : "You can only vote once per hour.", Toast.LENGTH_LONG).show();
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
        boolean isACorrect = "A".equalsIgnoreCase(correctOption);
        boolean isBCorrect = "B".equalsIgnoreCase(correctOption);
        boolean isCCorrect = "C".equalsIgnoreCase(correctOption);
        boolean isDCorrect = "D".equalsIgnoreCase(correctOption);

        showDistributionBar(holder.pbOptionA, holder.tvPctA, question.getOptionAPct(), isACorrect, context);
        showDistributionBar(holder.pbOptionB, holder.tvPctB, question.getOptionBPct(), isBCorrect, context);
        showDistributionBar(holder.pbOptionC, holder.tvPctC, question.getOptionCPct(), isCCorrect, context);
        showDistributionBar(holder.pbOptionD, holder.tvPctD, question.getOptionDPct(), isDCorrect, context);

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

        int mutedColor = ContextCompat.getColor(context, R.color.text_secondary);
        holder.tvPctA.setTextColor(mutedColor);
        holder.tvPctA.setTypeface(null, Typeface.NORMAL);
        holder.tvPctA.setVisibility(View.GONE);

        holder.tvPctB.setTextColor(mutedColor);
        holder.tvPctB.setTypeface(null, Typeface.NORMAL);
        holder.tvPctB.setVisibility(View.GONE);

        holder.tvPctC.setTextColor(mutedColor);
        holder.tvPctC.setTypeface(null, Typeface.NORMAL);
        holder.tvPctC.setVisibility(View.GONE);

        holder.tvPctD.setTextColor(mutedColor);
        holder.tvPctD.setTypeface(null, Typeface.NORMAL);
        holder.tvPctD.setVisibility(View.GONE);

        holder.layoutExplanation.setVisibility(View.GONE);
    }

    private void showDistributionBar(ProgressBar pb, TextView tvPct, float percentage, boolean isCorrect, Context context) {
        pb.setVisibility(View.VISIBLE);
        tvPct.setVisibility(View.VISIBLE);
        int intPct = Math.min(100, Math.max(0, Math.round(percentage)));
        pb.setMax(100);
        pb.setProgress(intPct);
        tvPct.setText(intPct + "%");

        int tintColor = ContextCompat.getColor(context, isCorrect ? R.color.diff_easy : R.color.primary);
        pb.setProgressTintList(ColorStateList.valueOf(tintColor));
        if (isCorrect) {
            tvPct.setTextColor(tintColor);
            tvPct.setTypeface(null, Typeface.BOLD);
        } else {
            tvPct.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            tvPct.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void bindVoting(CardViewHolder holder, Question question, Context context) {
        holder.tvUpvotes.setText(String.valueOf(question.getUpvotesCount()));
        holder.tvDownvotes.setText(String.valueOf(question.getDownvotesCount()));

        String userVoted = question.getUserVoted();
        if ("up".equalsIgnoreCase(userVoted) || "like".equalsIgnoreCase(userVoted)) {
            holder.btnUpvote.setColorFilter(ContextCompat.getColor(context, R.color.primary));
            holder.tvUpvotes.setTextColor(ContextCompat.getColor(context, R.color.primary));
            holder.tvUpvotes.setTypeface(null, Typeface.BOLD);
            if (holder.layoutBtnUpvote != null) {
                holder.layoutBtnUpvote.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_surface)));
            }

            holder.btnDownvote.setColorFilter(ContextCompat.getColor(context, R.color.text_muted));
            holder.tvDownvotes.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvDownvotes.setTypeface(null, Typeface.NORMAL);
            if (holder.layoutBtnDownvote != null) {
                holder.layoutBtnDownvote.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_variant)));
            }
        } else if ("down".equalsIgnoreCase(userVoted) || "unlike".equalsIgnoreCase(userVoted)) {
            holder.btnUpvote.setColorFilter(ContextCompat.getColor(context, R.color.text_muted));
            holder.tvUpvotes.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvUpvotes.setTypeface(null, Typeface.NORMAL);
            if (holder.layoutBtnUpvote != null) {
                holder.layoutBtnUpvote.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_variant)));
            }

            holder.btnDownvote.setColorFilter(ContextCompat.getColor(context, R.color.error));
            holder.tvDownvotes.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.tvDownvotes.setTypeface(null, Typeface.BOLD);
            if (holder.layoutBtnDownvote != null) {
                holder.layoutBtnDownvote.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.error_light)));
            }
        } else {
            holder.btnUpvote.setColorFilter(ContextCompat.getColor(context, R.color.text_muted));
            holder.tvUpvotes.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvUpvotes.setTypeface(null, Typeface.NORMAL);
            if (holder.layoutBtnUpvote != null) {
                holder.layoutBtnUpvote.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_variant)));
            }

            holder.btnDownvote.setColorFilter(ContextCompat.getColor(context, R.color.text_muted));
            holder.tvDownvotes.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvDownvotes.setTypeface(null, Typeface.NORMAL);
            if (holder.layoutBtnDownvote != null) {
                holder.layoutBtnDownvote.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_variant)));
            }
        }
    }

    private boolean isShortOption(String text) {
        return text != null && text.trim().length() <= 24 && !text.contains("\n");
    }

    private void configureAdaptiveOptions(CardViewHolder holder, boolean isShort, Context context) {
        if (holder.layoutRowAB == null || holder.layoutRowCD == null) return;
        int margin4dp = (int) (4 * context.getResources().getDisplayMetrics().density);
        int margin6dp = (int) (6 * context.getResources().getDisplayMetrics().density);

        if (isShort) {
            holder.layoutRowAB.setOrientation(LinearLayout.HORIZONTAL);
            holder.layoutRowCD.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lpA.setMarginEnd(margin4dp);
            holder.layoutOptionA.setLayoutParams(lpA);

            LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lpB.setMarginStart(margin4dp);
            holder.layoutOptionB.setLayoutParams(lpB);

            LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lpC.setMarginEnd(margin4dp);
            holder.layoutOptionC.setLayoutParams(lpC);

            LinearLayout.LayoutParams lpD = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            lpD.setMarginStart(margin4dp);
            holder.layoutOptionD.setLayoutParams(lpD);
        } else {
            holder.layoutRowAB.setOrientation(LinearLayout.VERTICAL);
            holder.layoutRowCD.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            holder.layoutOptionA.setLayoutParams(lpA);

            LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpB.topMargin = margin6dp;
            holder.layoutOptionB.setLayoutParams(lpB);

            LinearLayout.LayoutParams lpC = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            holder.layoutOptionC.setLayoutParams(lpC);

            LinearLayout.LayoutParams lpD = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpD.topMargin = margin6dp;
            holder.layoutOptionD.setLayoutParams(lpD);
        }
    }

    private void bindStar(ImageView ivStar, boolean isStarred, Context context) {
        ivStar.setColorFilter(ContextCompat.getColor(context, isStarred ? R.color.amber_accent : R.color.text_muted));
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        MaterialCheckBox cbSelectQuestion;
        TextView tvSubjectBadge;
        TextView tvDifficultyBadge;
        TextView tvPrevAnswerBadge;
        Button btnCreatorReveal;
        ImageView btnStar;
        TextView tvQuestionText;
        ImageView ivQuestionImage;

        LinearLayout layoutRowAB;
        LinearLayout layoutRowCD;

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
        LinearLayout layoutBtnUpvote;
        ImageView btnUpvote;
        TextView tvUpvotes;
        LinearLayout layoutBtnDownvote;
        ImageView btnDownvote;
        TextView tvDownvotes;

        LinearLayout btnComments;
        TextView tvCommentCount;
        ImageView btnReport;

        CardViewHolder(View itemView) {
            super(itemView);
            cbSelectQuestion = itemView.findViewById(R.id.cbSelectQuestion);
            tvSubjectBadge = itemView.findViewById(R.id.tvSubjectBadge);
            tvDifficultyBadge = itemView.findViewById(R.id.tvDifficultyBadge);
            tvPrevAnswerBadge = itemView.findViewById(R.id.tvPrevAnswerBadge);
            btnCreatorReveal = itemView.findViewById(R.id.btnCreatorReveal);
            btnStar = itemView.findViewById(R.id.btnStar);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            ivQuestionImage = itemView.findViewById(R.id.ivQuestionImage);
            layoutRowAB = itemView.findViewById(R.id.layoutRowAB);
            layoutRowCD = itemView.findViewById(R.id.layoutRowCD);

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
            layoutBtnUpvote = itemView.findViewById(R.id.layoutBtnUpvote);
            btnUpvote = itemView.findViewById(R.id.btnUpvote);
            tvUpvotes = itemView.findViewById(R.id.tvUpvotes);
            layoutBtnDownvote = itemView.findViewById(R.id.layoutBtnDownvote);
            btnDownvote = itemView.findViewById(R.id.btnDownvote);
            tvDownvotes = itemView.findViewById(R.id.tvDownvotes);

            btnComments = itemView.findViewById(R.id.btnComments);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            btnReport = itemView.findViewById(R.id.btnReport);
        }
    }
}
