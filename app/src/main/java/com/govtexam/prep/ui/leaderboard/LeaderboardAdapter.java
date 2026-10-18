package com.govtexam.prep.ui.leaderboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.govtexam.prep.R;
import com.govtexam.prep.data.model.LeaderboardEntry;

import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for rendering rank-ordered student entries on global leaderboards.
 */
public class LeaderboardAdapter extends ListAdapter<LeaderboardEntry, LeaderboardAdapter.LeaderboardViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(LeaderboardEntry entry);
    }

    private final OnUserClickListener userClickListener;

    public LeaderboardAdapter(OnUserClickListener userClickListener) {
        super(DIFF_CALLBACK);
        this.userClickListener = userClickListener;
    }

    private static final DiffUtil.ItemCallback<LeaderboardEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<LeaderboardEntry>() {
                @Override
                public boolean areItemsTheSame(@NonNull LeaderboardEntry oldItem, @NonNull LeaderboardEntry newItem) {
                    return oldItem.getUserId() == newItem.getUserId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull LeaderboardEntry oldItem, @NonNull LeaderboardEntry newItem) {
                    return oldItem.getRank() == newItem.getRank() &&
                            Double.compare(oldItem.getScore(), newItem.getScore()) == 0 &&
                            Objects.equals(oldItem.getName(), newItem.getName()) &&
                            Objects.equals(oldItem.getTierBadge(), newItem.getTierBadge());
                }
            };

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        LeaderboardEntry entry = getItem(position);
        holder.bind(entry, userClickListener);
    }

    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRank;
        private final ImageView imgAvatar;
        private final TextView tvName;
        private final TextView tvUsername;
        private final TextView tvTierBadge;
        private final TextView tvScore;
        private final TextView tvExamsCount;
        private final TextView tvDifficultyRatio;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvTierBadge = itemView.findViewById(R.id.tvTierBadge);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvExamsCount = itemView.findViewById(R.id.tvExamsCount);
            tvDifficultyRatio = itemView.findViewById(R.id.tvDifficultyRatio);
        }

        public void bind(LeaderboardEntry entry, OnUserClickListener listener) {
            Context context = itemView.getContext();

            tvRank.setText(String.format(Locale.getDefault(), "#%d", entry.getRank()));
            tvName.setText(entry.getName());
            tvUsername.setText(String.format("@%s", entry.getUsername()));
            tvTierBadge.setText(entry.getTierBadge());
            tvScore.setText(String.format(Locale.US, "%.1f pts", entry.getScore()));

            if (entry.getExamsAttendedCount() > 0) {
                tvExamsCount.setVisibility(View.VISIBLE);
                tvExamsCount.setText(String.format(Locale.getDefault(), "%d exams attended", entry.getExamsAttendedCount()));
            } else {
                tvExamsCount.setVisibility(View.GONE);
            }

            if (entry.getDifficultyBadge() != null && !entry.getDifficultyBadge().isEmpty()) {
                tvDifficultyRatio.setVisibility(View.VISIBLE);
                tvDifficultyRatio.setText(entry.getDifficultyBadge());
            } else if (entry.getHardRatio() > 0) {
                tvDifficultyRatio.setVisibility(View.VISIBLE);
                tvDifficultyRatio.setText(String.format(Locale.US, "Hard: %.0f%%", entry.getHardRatio() * 100));
            } else {
                tvDifficultyRatio.setVisibility(View.GONE);
            }

            if (entry.getAvatarUrl() != null && !entry.getAvatarUrl().isEmpty()) {
                Glide.with(context)
                        .load(entry.getAvatarUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .placeholder(R.drawable.ic_app_logo)
                        .error(R.drawable.ic_app_logo)
                        .into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_app_logo);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(entry);
                }
            });
        }
    }
}
