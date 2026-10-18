package com.questions.govt.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.questions.govt.R;
import com.questions.govt.data.model.TaxonomyItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for modern Web3 card-based subject grid view in the navigation drawer.
 */
public class DrawerSubjectCardAdapter extends RecyclerView.Adapter<DrawerSubjectCardAdapter.ViewHolder> {

    private final List<TaxonomyItem> subjects = new ArrayList<>();
    private final OnSubjectClickListener listener;

    public interface OnSubjectClickListener {
        void onSubjectClick(TaxonomyItem subject);
    }

    public DrawerSubjectCardAdapter(OnSubjectClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<TaxonomyItem> newSubjects) {
        subjects.clear();
        if (newSubjects != null) {
            subjects.addAll(newSubjects);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drawer_subject_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaxonomyItem subject = subjects.get(position);
        holder.tvTitle.setText(subject.getTitle());
        holder.tvCount.setText(subject.getQuestionsCount() + " MCQs");

        Glide.with(holder.itemView.getContext())
                .load(subject.getIconUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_app_logo)
                .error(R.drawable.ic_app_logo)
                .into(holder.ivIcon);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSubjectClick(subject);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvCount;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivSubjectCardIcon);
            tvTitle = itemView.findViewById(R.id.tvSubjectCardTitle);
            tvCount = itemView.findViewById(R.id.tvSubjectCardCount);
        }
    }
}
