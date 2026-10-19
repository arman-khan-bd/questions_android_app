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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reusable Nested Comment Adapter supporting top-level comments and multi-level replies
 * with a 10-reply display limit and "See more replies" expand/collapse toggle.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    public interface OnReplyClickListener {
        void onReply(Comment comment);
    }

    private final List<Comment> items;
    private final OnReplyClickListener listener;
    private final Set<Long> expandedCommentIds;

    public CommentAdapter(List<Comment> items, OnReplyClickListener listener) {
        this(items, listener, new HashSet<>());
    }

    public CommentAdapter(List<Comment> items, OnReplyClickListener listener, Set<Long> expandedCommentIds) {
        this.items = items;
        this.listener = listener;
        this.expandedCommentIds = expandedCommentIds != null ? expandedCommentIds : new HashSet<>();
    }

    public void expandCommentReplies(long commentId) {
        expandedCommentIds.add(commentId);
        notifyDataSetChanged();
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

        // Nested replies (Level 2+) with 10-reply limit and "See more replies" expansion
        List<Comment> replies = comment.getReplies();
        if (replies != null && !replies.isEmpty()) {
            if (holder.layoutRepliesContainer != null) {
                holder.layoutRepliesContainer.setVisibility(View.VISIBLE);
            }
            holder.rvReplies.setVisibility(View.VISIBLE);

            int totalReplies = replies.size();
            boolean isExpanded = expandedCommentIds.contains(comment.getId());
            List<Comment> displayReplies;

            if (totalReplies > 10) {
                if (holder.btnSeeMoreReplies != null) {
                    holder.btnSeeMoreReplies.setVisibility(View.VISIBLE);
                    if (isExpanded) {
                        displayReplies = replies;
                        holder.btnSeeMoreReplies.setText("▲ Show fewer replies");
                    } else {
                        displayReplies = replies.subList(0, 10);
                        int remaining = totalReplies - 10;
                        holder.btnSeeMoreReplies.setText("── View " + remaining + " more " + (remaining == 1 ? "reply" : "replies"));
                    }

                    holder.btnSeeMoreReplies.setOnClickListener(v -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            if (expandedCommentIds.contains(comment.getId())) {
                                expandedCommentIds.remove(comment.getId());
                            } else {
                                expandedCommentIds.add(comment.getId());
                            }
                            notifyItemChanged(pos);
                        }
                    });
                } else {
                    displayReplies = replies;
                }
            } else {
                displayReplies = replies;
                if (holder.btnSeeMoreReplies != null) {
                    holder.btnSeeMoreReplies.setVisibility(View.GONE);
                }
            }

            holder.rvReplies.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            holder.rvReplies.setAdapter(new CommentAdapter(displayReplies, listener, expandedCommentIds));
        } else {
            if (holder.layoutRepliesContainer != null) {
                holder.layoutRepliesContainer.setVisibility(View.GONE);
            }
            if (holder.btnSeeMoreReplies != null) {
                holder.btnSeeMoreReplies.setVisibility(View.GONE);
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
        TextView btnSeeMoreReplies;

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
            btnSeeMoreReplies = itemView.findViewById(R.id.btnSeeMoreReplies);
        }
    }
}
