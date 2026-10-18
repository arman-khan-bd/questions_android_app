package com.questions.govt.ui.question;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.questions.govt.R;
import com.questions.govt.data.model.Comment;
import com.questions.govt.data.remote.ApiClient;
import com.questions.govt.data.remote.response.ApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * BottomSheetDialogFragment displaying 2-level nested comments with backend bad-word moderation awareness.
 */
public class CommentBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String ARG_QUESTION_ID = "arg_question_id";

    private long questionId;
    private Long replyParentId = null;

    private TextView tvCommentsTitle;
    private ImageView ivCloseComments;
    private RecyclerView rvComments;
    private LinearLayout layoutReplyingTo;
    private TextView tvReplyingTo;
    private ImageView ivCancelReply;
    private EditText etCommentInput;
    private Button btnPostComment;

    private CommentAdapter adapter;
    private final List<Comment> commentList = new ArrayList<>();

    public static CommentBottomSheetFragment newInstance(long questionId) {
        CommentBottomSheetFragment fragment = new CommentBottomSheetFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_QUESTION_ID, questionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            questionId = getArguments().getLong(ARG_QUESTION_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadComments();
    }

    private void initViews(View view) {
        tvCommentsTitle = view.findViewById(R.id.tvCommentsTitle);
        ivCloseComments = view.findViewById(R.id.ivCloseComments);
        rvComments = view.findViewById(R.id.rvComments);
        layoutReplyingTo = view.findViewById(R.id.layoutReplyingTo);
        tvReplyingTo = view.findViewById(R.id.tvReplyingTo);
        ivCancelReply = view.findViewById(R.id.ivCancelReply);
        etCommentInput = view.findViewById(R.id.etCommentInput);
        btnPostComment = view.findViewById(R.id.btnPostComment);

        ivCloseComments.setOnClickListener(v -> dismiss());

        ivCancelReply.setOnClickListener(v -> {
            replyParentId = null;
            layoutReplyingTo.setVisibility(View.GONE);
            etCommentInput.setHint("Add a polite comment or question…");
        });

        adapter = new CommentAdapter(commentList, comment -> {
            replyParentId = comment.getId();
            layoutReplyingTo.setVisibility(View.VISIBLE);
            tvReplyingTo.setText("Replying to @" + comment.getUserUsername());
            etCommentInput.setHint("Reply to @" + comment.getUserUsername() + "…");
            etCommentInput.requestFocus();
        });

        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(adapter);

        btnPostComment.setOnClickListener(v -> submitComment());
    }

    private void loadComments() {
        ApiClient.getApiService().getComments(questionId).enqueue(new Callback<ApiResponse<List<Comment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Comment>>> call, Response<ApiResponse<List<Comment>>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    commentList.clear();
                    commentList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    tvCommentsTitle.setText("Comments (" + commentList.size() + ")");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Comment>>> call, Throwable t) {}
        });
    }

    private void submitComment() {
        String text = etCommentInput.getText() != null ? etCommentInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        btnPostComment.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("comment_text", text);

        Call<ApiResponse<Comment>> call;
        if (replyParentId != null) {
            call = ApiClient.getApiService().postReply(replyParentId, body);
        } else {
            call = ApiClient.getApiService().postComment(questionId, body);
        }

        call.enqueue(new Callback<ApiResponse<Comment>>() {
            @Override
            public void onResponse(Call<ApiResponse<Comment>> call, Response<ApiResponse<Comment>> response) {
                btnPostComment.setEnabled(true);

                if (response.code() == 422) {
                    // Backend BadWordFilterService detected offensive profanity
                    Toast.makeText(requireContext(), "Profanity Detected: Your comment violates community guidelines.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Comment posted = response.body().getData();
                    etCommentInput.setText("");
                    replyParentId = null;
                    layoutReplyingTo.setVisibility(View.GONE);

                    if ("flagged_pending".equalsIgnoreCase(posted.getModerationStatus())) {
                        Toast.makeText(requireContext(), "Comment submitted! Held for Admin Review due to flagged words.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Comment posted!", Toast.LENGTH_SHORT).show();
                    }

                    loadComments();
                } else {
                    Toast.makeText(requireContext(), "Failed to post comment.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Comment>> call, Throwable t) {
                btnPostComment.setEnabled(true);
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
        private final List<Comment> items;
        private final OnReplyClickListener listener;

        interface OnReplyClickListener {
            void onReply(Comment comment);
        }

        CommentAdapter(List<Comment> items, OnReplyClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Comment comment = items.get(position);
            holder.tvCommentUser.setText(comment.getUserName() + " (" + comment.getUserUsername() + ")");
            holder.tvCommentDate.setText(comment.getCreatedAt());

            // Check bad-word moderation status
            String modStatus = comment.getModerationStatus();
            if ("flagged_pending".equalsIgnoreCase(modStatus)) {
                holder.tvModerationBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvModerationBadge.setVisibility(View.GONE);
            }

            holder.tvCommentBody.setText(comment.getCommentText());
            holder.btnReply.setOnClickListener(v -> {
                if (listener != null) listener.onReply(comment);
            });

            // Level 2 replies
            if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
                holder.rvReplies.setVisibility(View.VISIBLE);
                holder.rvReplies.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
                holder.rvReplies.setAdapter(new CommentAdapter(comment.getReplies(), listener));
            } else {
                holder.rvReplies.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCommentUser;
            TextView tvCommentDate;
            TextView tvModerationBadge;
            TextView tvCommentBody;
            TextView btnReply;
            RecyclerView rvReplies;

            ViewHolder(View itemView) {
                super(itemView);
                tvCommentUser = itemView.findViewById(R.id.tvCommentUser);
                tvCommentDate = itemView.findViewById(R.id.tvCommentDate);
                tvModerationBadge = itemView.findViewById(R.id.tvModerationBadge);
                tvCommentBody = itemView.findViewById(R.id.tvCommentBody);
                btnReply = itemView.findViewById(R.id.btnReply);
                rvReplies = itemView.findViewById(R.id.rvReplies);
            }
        }
    }
}
