package com.questions.govt.ui.question;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

        // Bind user avatar with Glide circular crop
        if (holder.ivCommentAvatar != null) {
            String avatarUrl = comment.getUserAvatarUrl();
            if (!TextUtils.isEmpty(avatarUrl)) {
                Glide.with(holder.itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_nav_profile)
                        .error(R.drawable.ic_nav_profile)
                        .into(holder.ivCommentAvatar);
            } else {
                holder.ivCommentAvatar.setImageResource(R.drawable.ic_nav_profile);
            }
        }

        holder.tvCommentUser.setText(!TextUtils.isEmpty(comment.getUserName()) ? comment.getUserName() : "Student");
        if (holder.tvCommentHandle != null) {
            String handle = comment.getUserUsername();
            if (!TextUtils.isEmpty(handle)) {
                holder.tvCommentHandle.setText("@" + handle);
                holder.tvCommentHandle.setVisibility(View.VISIBLE);
            } else {
                holder.tvCommentHandle.setVisibility(View.GONE);
            }
        }
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

        // Nested replies (Level 2+) with thread layout
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            if (holder.layoutRepliesContainer != null) {
                holder.layoutRepliesContainer.setVisibility(View.VISIBLE);
            }
            holder.rvReplies.setVisibility(View.VISIBLE);
            holder.rvReplies.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            holder.rvReplies.setAdapter(new CommentAdapter(comment.getReplies(), listener));
        } else {
            if (holder.layoutRepliesContainer != null) {
                holder.layoutRepliesContainer.setVisibility(View.GONE);
            }
            holder.rvReplies.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCommentAvatar;
        TextView tvCommentUser;
        TextView tvCommentHandle;
        TextView tvCommentDate;
        TextView tvModerationBadge;
        TextView tvCommentBody;
        TextView btnReply;
        LinearLayout layoutRepliesContainer;
        RecyclerView rvReplies;

        ViewHolder(View itemView) {
            super(itemView);
            ivCommentAvatar = itemView.findViewById(R.id.ivCommentAvatar);
            tvCommentUser = itemView.findViewById(R.id.tvCommentUser);
            tvCommentHandle = itemView.findViewById(R.id.tvCommentHandle);
            tvCommentDate = itemView.findViewById(R.id.tvCommentDate);
            tvModerationBadge = itemView.findViewById(R.id.tvModerationBadge);
            tvCommentBody = itemView.findViewById(R.id.tvCommentBody);
            btnReply = itemView.findViewById(R.id.btnReply);
            layoutRepliesContainer = itemView.findViewById(R.id.layoutRepliesContainer);
            rvReplies = itemView.findViewById(R.id.rvReplies);
        }
    }
}
