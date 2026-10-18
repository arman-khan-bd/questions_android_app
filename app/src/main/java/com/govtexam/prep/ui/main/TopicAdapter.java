package com.govtexam.prep.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.govtexam.prep.R;
import com.govtexam.prep.data.model.TaxonomyItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal RecyclerView adapter for Topics.
 */
public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {

    private final List<TaxonomyItem> topics = new ArrayList<>();
    private final OnTopicClickListener listener;

    public interface OnTopicClickListener {
        void onTopicClick(TaxonomyItem topic);
    }

    public TopicAdapter(OnTopicClickListener listener) {
        this.listener = listener;
    }

    public void setTopics(List<TaxonomyItem> newTopics) {
        topics.clear();
        if (newTopics != null) {
            topics.addAll(newTopics);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_topic_horizontal, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        TaxonomyItem topic = topics.get(position);
        holder.tvTopicTitle.setText(topic.getTitle());
        holder.tvTopicCount.setText(topic.getQuestionsCount() + " MCQs");

        Glide.with(holder.itemView.getContext())
                .load(topic.getIconUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_app_logo)
                .error(R.drawable.ic_app_logo)
                .into(holder.ivTopicIcon);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTopicClick(topic);
            }
        });
    }

    @Override
    public int getItemCount() {
        return topics.size();
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTopicIcon;
        TextView tvTopicTitle;
        TextView tvTopicCount;

        TopicViewHolder(View itemView) {
            super(itemView);
            ivTopicIcon = itemView.findViewById(R.id.ivTopicIcon);
            tvTopicTitle = itemView.findViewById(R.id.tvTopicTitle);
            tvTopicCount = itemView.findViewById(R.id.tvTopicCount);
        }
    }
}
