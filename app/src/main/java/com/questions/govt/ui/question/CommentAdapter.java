package com.questions.govt.ui.question;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.questions.govt.R;
import com.questions.govt.data.model.Comment;

import java.util.List;

/**
 * Reusable Nested Comment Adapter supporting top-level comments and multi-level replies.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    public interface OnReplyClickListener {
        void onReply(Comment comment);
    }

    private final List<Comment> items;
    private final OnReplyClickListener listener;

    public CommentAdapter(List<Comment> items, OnReplyClickListener listener) {
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
        holder.tvCommentUser.setText(comment.getUserName() + " (@" + comment.getUserUsername() + ")");
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
            if (listener != null) {
                listener.onReply(comment);
            }
        });

        // Nested replies (Level 2+)
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
        return items != null ? items.size() : 0;
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
