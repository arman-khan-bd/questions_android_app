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
 * Grid RecyclerView adapter for All Subjects with .webp thumbnails.
 */
public class SubjectGridAdapter extends RecyclerView.Adapter<SubjectGridAdapter.SubjectViewHolder> {

    private final List<TaxonomyItem> subjects = new ArrayList<>();
    private final OnSubjectClickListener listener;

    public interface OnSubjectClickListener {
        void onSubjectClick(TaxonomyItem subject);
    }

    public SubjectGridAdapter(OnSubjectClickListener listener) {
        this.listener = listener;
    }

    public void setSubjects(List<TaxonomyItem> newSubjects) {
        subjects.clear();
        if (newSubjects != null) {
            subjects.addAll(newSubjects);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject_grid, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        TaxonomyItem subject = subjects.get(position);
        holder.tvSubjectTitle.setText(subject.getTitle());
        holder.tvSubjectQuestionsCount.setText(subject.getQuestionsCount() + " Questions");

        Glide.with(holder.itemView.getContext())
                .load(subject.getThumbnailUrl())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.bg_splash_gradient)
                .error(R.drawable.bg_splash_gradient)
                .into(holder.ivSubjectThumbnail);

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

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSubjectThumbnail;
        TextView tvSubjectTitle;
        TextView tvSubjectQuestionsCount;

        SubjectViewHolder(View itemView) {
            super(itemView);
            ivSubjectThumbnail = itemView.findViewById(R.id.ivSubjectThumbnail);
            tvSubjectTitle = itemView.findViewById(R.id.tvSubjectTitle);
            tvSubjectQuestionsCount = itemView.findViewById(R.id.tvSubjectQuestionsCount);
        }
    }
}
